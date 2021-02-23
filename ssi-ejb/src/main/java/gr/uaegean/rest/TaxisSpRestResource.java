package gr.uaegean.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import gr.uaegean.pojo.KeycloakSessionTO;
import gr.uaegean.services.PropertiesService;
import gr.uaegean.singleton.MemcacheSingleton;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.OIDCRedirectUriBuilder;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.springframework.util.StringUtils;

@ApplicationScoped
@Slf4j
public class TaxisSpRestResource {

    private final KeycloakSession session;

    @SuppressWarnings("unused")
    private final AuthenticationManager.AuthResult auth;

    private PropertiesService propServ;

//    private final static Logger LOG = LoggerFactory.getLogger(TaxisSpRestResource.class);

    private MemcachedClient mcc;

    public TaxisSpRestResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager().authenticateBearerToken(session, session.getContext().getRealm());
        this.propServ = new PropertiesService();

    }


    @GET
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
    @Path("callback")
    public Response taxisCallback(
            @Context HttpServletRequest httpServletRequest,
            @Context HttpServletResponse httpServletResponse,
            @QueryParam("code") String code,
            @QueryParam("state") String sessionId
    ) throws URISyntaxException, JsonProcessingException, IOException {

        this.mcc = MemcacheSingleton.getCache();
        log.info("code" + code);
        log.info("state" + sessionId);
        KeycloakSessionTO ksTo = (KeycloakSessionTO) mcc.get(sessionId);
        OIDCResponseMode responseMode = OIDCResponseMode.QUERY;
        String proceedWithAuthenticationUrl = this.propServ.getProp("TAXIS_AUTH_PROCEED"); //https://dss1.aegean.gr/auth/realms/SSI/protocol/openid-connect/auth
        log.info("will proceed to " + proceedWithAuthenticationUrl);
        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(proceedWithAuthenticationUrl, responseMode);
        log.info("the response type is set to {}", ksTo.getResponseType());
        redirectUri.addParam(OAuth2Constants.RESPONSE_TYPE, ksTo.getResponseType());
        redirectUri.addParam(OAuth2Constants.CLIENT_ID, ksTo.getClientId());
        redirectUri.addParam(OAuth2Constants.REDIRECT_URI, ksTo.getClientRedirectUri());
        redirectUri.addParam(OAuth2Constants.STATE, ksTo.getState());
        redirectUri.addParam(OAuth2Constants.SCOPE, ksTo.getScope());
        if (!StringUtils.isEmpty(ksTo.getNonce())) {
            redirectUri.addParam("nonce", ksTo.getNonce());
        }
        if (!StringUtils.isEmpty(ksTo.getResponseMode())) {
            redirectUri.addParam("response_mode", ksTo.getResponseMode());
        }

        redirectUri.addParam("sessionId", sessionId);
        redirectUri.addParam("code", code);
        redirectUri.addParam("ip", URLEncoder.encode(httpServletRequest.getRemoteAddr(), StandardCharsets.UTF_8.toString()));
        log.info("Proceeding ");
        return redirectUri.build();
    }

}
