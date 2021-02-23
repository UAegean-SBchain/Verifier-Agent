package gr.uaegean.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uaegean.pojo.KeycloakSessionTO;
import gr.uaegean.pojo.UportResponse;
import gr.uaegean.services.PropertiesService;
import gr.uaegean.singleton.MemcacheSingleton;
import gr.uaegean.utils.RealmUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@ApplicationScoped
@Slf4j
public class SsiSpRestResource {

    private final KeycloakSession session;

    private static Sse sse;
    private static SseBroadcaster SSE_BROADCASTER;
    private static OutboundSseEvent.Builder EVENT_BUILDER;
//    private static MinEduSingleton MINEDUSING;

    @SuppressWarnings("unused")
    private final AuthenticationManager.AuthResult auth;

    private PropertiesService propServ;

    private final static Logger LOG = LoggerFactory.getLogger(SsiSpRestResource.class);

    private MemcachedClient mcc;

    @Context
    public void setSse(Sse theSse) {
        sse = theSse;
        EVENT_BUILDER = sse.newEventBuilder();
        SSE_BROADCASTER = sse.newBroadcaster();
    }

    public SsiSpRestResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager().authenticateBearerToken(session, session.getContext().getRealm());

        this.propServ = new PropertiesService();

    }

    /**
     * POST to uPort SDK helper service @ /connectionResponse that service
     * verifies the received JWT and responds to a second endpoint of the
     * SsiSpResResource: / ssiResponse
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param jwt
     * @param ssiSessionId
     * @return
     * @throws URISyntaxException
     * @throws JsonProcessingException
     * @throws IOException
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Path("uportResponse")
    public Response processUportResponse(@Context HttpServletRequest httpServletRequest,
            @Context HttpServletResponse httpServletResponse, UportResponse jwt, @QueryParam("ssiSessionId") String ssiSessionId) throws URISyntaxException, JsonProcessingException, IOException {

//        LOG.info("I got the message" + jwt.toString());
        LOG.info("GOT uportResponse");
        RestTemplate restTemplate = new RestTemplate();
        final String baseUrl = this.propServ.getProp("UPORTHELPER") + "/connectionResponse?ssiSessionId=" + ssiSessionId;
        URI uri = new URI(baseUrl);
        restTemplate.postForObject(uri, jwt, UportResponse.class);
        LOG.info("uport response sent to uporthelper");

        return Response.ok().build();
    }

    @GET
    @Path("subscribe")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void subscribe(@Context SseEventSink sseEventSink, @Context Sse sse)  {
        log.info("************subscribe called!!!");
        if (sseEventSink == null) {
            log.error("No sseEventSink");
            throw new IllegalStateException("No client connected.");
        }
        if (SSE_BROADCASTER == null) {
            this.setSse(sse);
            log.info("SSE_BROADCASTER Was null but is set");
        }
        SSE_BROADCASTER.register(sseEventSink);
    }

    /**
     * Receives the response from the uPort SDK helper, parses it and continues
     * the OIDC flow
     *
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
    @Path("ssiResponse")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void processSSIResponse(@Context HttpServletRequest httpServletRequest,
            @Context SseEventSink eventSink, @Context Sse sse,
            @FormParam("claims") String vcClaims,
            @FormParam("sessionId") String sessionId) throws  IOException {

        LOG.info("ssiResponse reached");
        LOG.info("claims" + vcClaims); // strigified JSON
        LOG.info("sessionId" + sessionId);

        // received SessionID
        // the sessionId is sent as a Server sent event, signifying that the user has authenticated
        // once authenticated they browser will  post a request containing that sessionID
        // which will be used to log the user in the session
        int expiresInSec = 180;
        this.mcc = MemcacheSingleton.getCache();
        LOG.info("ssiResponse:: will add with key: claims" + sessionId + " the VC " + vcClaims);
        mcc.add("claims" + sessionId, expiresInSec, vcClaims);
        if (SSE_BROADCASTER == null) {
            this.setSse(sse);
        }
        OutboundSseEvent sseEvent = EVENT_BUILDER
                .name("vc_received")
                .id(String.valueOf(sessionId))
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .data(String.class, sessionId)
                .reconnectDelay(3000)
                .comment("vc_received")
                .build();
        SSE_BROADCASTER.broadcast(sseEvent);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
    @Path("proceed")
    public Response proceed(@FormParam("sessionId") String sessionId) throws IOException {

        LOG.info("transformClaimsToIDCResponse!!!");
        LOG.info("I got session:" + sessionId);
        // retrieve from the cache the client attributes
        this.mcc = MemcacheSingleton.getCache();
        if (this.mcc.get(sessionId) == null) {
            LOG.error("ERROR:: for sessionId" + sessionId + " parameters cached where null");
        }

        KeycloakSessionTO ksTo = (KeycloakSessionTO) this.mcc.get(sessionId);
        LOG.info("proceed:: " + ksTo.toString());

        // see also keycloak OIDCLoginProtocolc lass method authenticated
        OIDCResponseMode responseMode = OIDCResponseMode.QUERY;
        String proceedWithAuthenticationUrl = this.propServ.getProp("AUTH_PROCEED"); //http://localhost:8080/auth/realms/test/protocol/openid-connect/auth
        if (ksTo.getRealm() != null) {
            proceedWithAuthenticationUrl = RealmUtils.updateRealm(proceedWithAuthenticationUrl, ksTo.getRealm());
        }

        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(proceedWithAuthenticationUrl, responseMode);
        LOG.info("the redirctUri is " + redirectUri.build().toString());

        redirectUri.addParam(OAuth2Constants.RESPONSE_TYPE, ksTo.getResponseType());
        redirectUri.addParam(OAuth2Constants.CLIENT_ID, ksTo.getClientId());
        redirectUri.addParam(OAuth2Constants.REDIRECT_URI, ksTo.getClientRedirectUri());
        if(ksTo.getState()!= null){
            redirectUri.addParam(OAuth2Constants.STATE, ksTo.getState());
        }
        redirectUri.addParam(OAuth2Constants.SCOPE, ksTo.getScope());
        redirectUri.addParam("sessionId", sessionId);
        if (!StringUtils.isEmpty(ksTo.getNonce())) {
            redirectUri.addParam("nonce", ksTo.getNonce());
        }

        ObjectMapper mapper = new ObjectMapper();

        LOG.info("proceed with SSI response concluded ok");

        return redirectUri.build();
    }

    @GET
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
    @Path("proceedMobile")
    public Response proceedMobile(@QueryParam("ssiSessionId") String sessionId,
            //            @FormParam("claims") String claims,
            @Context HttpServletRequest httpServletRequest,
            @Context UriInfo uriInfo) throws IOException {
//            String access_token) throws IOException, URISyntaxException {

        this.mcc = MemcacheSingleton.getCache();
        // retrieve from the cache the client attributes
        KeycloakSessionTO ksTo = (KeycloakSessionTO) mcc.get(sessionId);
//        LOG.info("accessToken " + access_token);
        LOG.info(ksTo.toString());

        // see also keycloak OIDCLoginProtocolc lass method authenticated
        OIDCResponseMode responseMode = OIDCResponseMode.QUERY;
        String proceedWithAuthenticationUrl = this.propServ.getProp("AUTH_PROCEED");  //http://localhost:8080/auth/realms/test/protocol/openid-connect/auth
        if (ksTo.getRealm() != null) {
            proceedWithAuthenticationUrl = RealmUtils.updateRealm(proceedWithAuthenticationUrl, ksTo.getRealm());
        }

        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(proceedWithAuthenticationUrl, responseMode);
        LOG.info("the redirctUri is " + redirectUri.build().toString());

//        ObjectMapper mapper = new ObjectMapper();
        LOG.info("looking for creds-" + sessionId);
//        String access_token = (String) this.mcc.get("token-" + String.valueOf(sessionId));
//
        String claims = (String) this.mcc.get("creds-" + String.valueOf(sessionId));

//        UPortAccessToken token = mapper.readValue(access_token, UPortAccessToken.class);
//
//        RestTemplate restTemplate = new RestTemplate();
//        final String baseUrl = this.propServ.getProp("UPORTHELPER") + "/parseConnectionResponse";
//        URI uri = new URI(baseUrl);
//        String claims = restTemplate.postForObject(uri, token, String.class);
        LOG.info("proceedMobile:: will add with key: claims" + String.valueOf(sessionId) + " the VC " + claims);
        int expiresInSec = 180;
        mcc.add("claims" + sessionId, expiresInSec, claims);

        redirectUri.addParam(OAuth2Constants.RESPONSE_TYPE, ksTo.getResponseType());
        redirectUri.addParam(OAuth2Constants.CLIENT_ID, ksTo.getClientId());
        redirectUri.addParam(OAuth2Constants.REDIRECT_URI, ksTo.getClientRedirectUri());
        redirectUri.addParam(OAuth2Constants.STATE, ksTo.getState());
        redirectUri.addParam(OAuth2Constants.SCOPE, ksTo.getScope());
        redirectUri.addParam("sessionId", sessionId);

        LOG.info("proceedMobile with SSI response concluded ok");

        return redirectUri.build();
    }

}
