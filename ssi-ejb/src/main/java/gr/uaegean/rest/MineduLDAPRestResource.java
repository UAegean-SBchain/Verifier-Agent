package gr.uaegean.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uaegean.pojo.AmkaAndMitroResponse;
import gr.uaegean.pojo.KeycloakSessionTO;
import gr.uaegean.pojo.MinEduAmkaResponse.AmkaResponse;
import gr.uaegean.pojo.MinEduFamilyStatusResponse;
import gr.uaegean.pojo.MinEduLDAPResponse;
import gr.uaegean.services.PropertiesService;
import gr.uaegean.singleton.MemcacheSingleton;
import gr.uaegean.singleton.MinEduSingleton;
import gr.uaegean.utils.RealmUtils;
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

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.*;
import javax.naming.*;
import javax.naming.directory.*;

@Slf4j
@ApplicationScoped
public class MineduLDAPRestResource {

    private final KeycloakSession session;
    private static MinEduSingleton MINEDUSING;

    @SuppressWarnings("unused")
    private final AuthenticationManager.AuthResult auth;

    private PropertiesService propServ;

//    private final static Logger LOG = LoggerFactory.getLogger(MineduLDAPRestResource.class);

    private MemcachedClient mcc;

    public MineduLDAPRestResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager().authenticateBearerToken(session, session.getContext().getRealm());

        this.propServ = new PropertiesService();

    }

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @param sessionId           the current sesionId of the user authentication
     * @return server error in case of errors, bad request if the user is not
     * found
     * <p>
     * OR the sessionid in case of * correct retrieval of user attributes. In
     * this case the response is cached with key the sessionId so that it can be
     * propaged to the authentication succeded endpoint of keycloak
     * @throws URISyntaxException
     * @throws JsonProcessingException
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED + ";charset=UTF-8")
    @Path("auth")
    public Response requestMitro(
            @Context HttpServletRequest httpServletRequest,
            @Context HttpServletResponse httpServletResponse,
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("sessionId") String sessionId) throws URISyntaxException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        httpServletRequest.setCharacterEncoding("UTF-8");
        username = new String(username.getBytes(), "UTF-8");
        log.info("will try to authenticate ldap user {} with pass {}", username, password);

        String url = "ldap://minedu.gov.gr:389";
        Hashtable env = new Hashtable();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, url);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.REFERRAL, "follow");
//        env.put(javax.naming.Context.SECURITY_PRINCIPAL, "digisignsa@minedu");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, username);
//        env.put(javax.naming.Context.SECURITY_CREDENTIALS, "j#5A2U!fdqQ=AgM.");
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

        try {
            DirContext ctx = new InitialDirContext(env);
            log.info("connected to the ldap");
            SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            //&(objectClass=*) (samaccountname="+username+")
            String queryCleanUserName = username.replace("@minedu", "");
            NamingEnumeration results =
                    ctx.search("DC=minedu,DC=gov,DC=gr", "(samaccountname=" + queryCleanUserName + ")", getSimpleSearchControls());

            log.info("there should be some results here");
            while (results != null && results.hasMore()) {
                // Display an entry
                SearchResult entry = (SearchResult) results.next();
                Attributes attrs = entry.getAttributes();
                NamingEnumeration attributes = attrs.getAll();

                MinEduLDAPResponse response = new MinEduLDAPResponse();
                while (attributes != null && attributes.hasMore()) {
                    Object nm = attributes.next();
                    log.info("attribute found;{}", nm.toString()); //e.g. attribute found: sAMAccountName: digisignsa
                    if (!StringUtils.isEmpty(nm.toString())) {
                        String[] attributeValue = nm.toString().split(":");
                        switch (attributeValue[0]) {
                            case "sAMAccountName":
                                response.setUsername(attributeValue[1].trim());
                                break;
                            case "givenName":
                                response.setName(attributeValue[1].trim());
                                break;
                            case "sn":
                                response.setSurname(attributeValue[1].trim());
                                break;
                            case "email":
                                response.setEmail(attributeValue[1].trim());
                                break;
                            default:
                                break;
                        }

                    }
                }
                MemcacheSingleton.getCache().add("mineduLDAP-" + sessionId, 1000, mapper.writeValueAsString(response));
            }
            // do something useful with the context...
            ctx.close();
            return Response.status(Response.Status.OK).entity(sessionId).build();


        } catch (AuthenticationNotSupportedException ex) {
            log.info("The authentication is not supported by the server");
        } catch (AuthenticationException ex) {
            log.info("incorrect password or username");
        } catch (NamingException ex) {
            log.info("error when trying to create the context" + ex);
        }


//        if (famStatus.isPresent() && amkaResp.isPresent()) {
//            LOG.info("MitroSpRest");
//            LOG.info(famStatus.toString());
//            AmkaAndMitroResponse bundledResponse = new AmkaAndMitroResponse(amkaResp.get(), famStatus.get());
//
//            MemcacheSingleton.getCache().add("mitro-" + sessionId, 1000, mapper.writeValueAsString(bundledResponse));
//            LOG.info("will cahce with key: " + sessionId);
//            return Response.status(Response.Status.OK).entity(sessionId).build();
//        }
        return Response.serverError().build();
    }

    @GET
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.APPLICATION_FORM_URLENCODED})
    @Path("proceed")
    public Response retriveOIDCParamAndProceed(@QueryParam("sessionId") String sessionId) throws IOException {
        log.info("proceed!!!");
        // see also keycloak OIDCLoginProtocolc lass method authenticated
        OIDCResponseMode responseMode = OIDCResponseMode.QUERY;
        String proceedWithAuthenticationUrl = this.propServ.getProp("MITRO_AUTH_PROCEED"); //http://localhost:8080/auth/realms/test/protocol/openid-connect/auth
        log.info("will procceed to " + proceedWithAuthenticationUrl);
        this.mcc = MemcacheSingleton.getCache();
        // retrieve from the cache the origianl OIDC call  paramters
        KeycloakSessionTO ksTo = (KeycloakSessionTO) mcc.get(sessionId);
        if (ksTo.getRealm() != null) {
            proceedWithAuthenticationUrl = RealmUtils.updateRealm(proceedWithAuthenticationUrl, ksTo.getRealm());
        }
        OIDCRedirectUriBuilder redirectUri = OIDCRedirectUriBuilder.fromUri(proceedWithAuthenticationUrl, responseMode);

        redirectUri.addParam(OAuth2Constants.RESPONSE_TYPE, ksTo.getResponseType());
        redirectUri.addParam(OAuth2Constants.CLIENT_ID, ksTo.getClientId());
        redirectUri.addParam(OAuth2Constants.REDIRECT_URI, ksTo.getClientRedirectUri());
        redirectUri.addParam(OAuth2Constants.STATE, ksTo.getState());
        redirectUri.addParam(OAuth2Constants.SCOPE, ksTo.getScope());
        redirectUri.addParam("sessionId", sessionId);
        if (!StringUtils.isEmpty(ksTo.getResponseMode())) {
            redirectUri.addParam("response_mode", ksTo.getResponseMode());
        }
        if (!StringUtils.isEmpty(ksTo.getNonce())) {
            redirectUri.addParam("nonce", ksTo.getNonce());
        }

        log.info("proceed concluded ok");
        return redirectUri.build();
    }


    public static SearchControls getSimpleSearchControls() {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setTimeLimit(30000);
        String[] attrIDs =
                {"SAMAccountName", "givenname", "sn", "mail"};
        //"SAMAccountName", "samaccountname","givenname","mail","cn", "sn"

        searchControls.setReturningAttributes(attrIDs);
        return searchControls;
    }

}
