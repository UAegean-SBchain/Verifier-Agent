/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import com.google.common.hash.Hashing;
import gr.uaegean.pojo.KeycloakSessionTO;
import gr.uaegean.services.PropertiesService;
import gr.uaegean.singleton.MemcacheSingleton;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 *
 * @author nikos
 */
@Slf4j
public class BeforeTaxisAuthenticator extends AbstractTaxisAuthenticator {

    @Override
    public void action(AuthenticationFlowContext afc) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession ks, RealmModel rm, UserModel um) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession ks, RealmModel rm, UserModel um) {
    }

    @Override
    public void close() {

    }

    @Override
    public void authenticateImpl(AuthenticationFlowContext context) {
        try {
//            KeycloakSession session = context.getSession();
//            RealmModel realm = context.getRealm();
            //    protected ParameterService paramServ = new ParameterServiceImpl();
            //    private static final Logger LOG = LoggerFactory.getLogger(BeforeTaxisAuthenticator.class);
            MemcachedClient mcc = MemcacheSingleton.getCache();
            PropertiesService propServ = new PropertiesService();
            // create a new sessionId
            String sessionId = UUID.randomUUID().toString();
            // grab oidc params
            String response_type = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.RESPONSE_TYPE);
            log.info("***** THE RESPONSE TYPE IS:: " + response_type);
            String client_id = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);
            log.info("***** THE CLINET_ID is :: " + client_id);

            ClientModel client = context.getRealm().getClientByClientId(client_id);
            if(client != null){
                String clientAuthenticatorType = client.getClientAuthenticatorType();
                client.getAttributes().forEach( (name,value) -> {
                    log.info("******* client attribute-{} has value-{}",name,value);
                });
                log.info("*************** clientAuthenticatorType {}",clientAuthenticatorType );
            }else{
                log.error("NO CLIENT found for id, {}",client_id);
            }



            String redirect_uri = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
            String state = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
            String scope = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.SCOPE);
            String response_mode = context.getHttpRequest().getUri().getQueryParameters().getFirst("response_mode");
            String nonce = context.getHttpRequest().getUri().getQueryParameters().getFirst("nonce");
            int expiresInSec = 300;
            //Transfer Object that will be cached
            KeycloakSessionTO ksTO = new KeycloakSessionTO(state, response_type,
                    client_id, redirect_uri, state, scope, null, nonce,response_mode);
            mcc.add(sessionId, expiresInSec, ksTO);
            // storing the hash of the sessionId as that will be sent back after the auathentication (as the session with the taxis service)
            // this way the actuall sessionId can be retrieved from the received hash
            String hahsedChallenge = Hashing.sha256()
                    .hashString(sessionId, StandardCharsets.UTF_8)
                    .toString();
            log.info("BeforeTaxisAuthenticator:: will store the hashedChallenge:: " + hahsedChallenge);
            mcc.add(hahsedChallenge, expiresInSec, sessionId);
            //

            /**
             * "clientId" => env("GSIS_OAUTH2_CLIENT_ID"), "clientSecret" =>
             * env("GSIS_OAUTH2_CLIENT_SECRET"), "redirectUri" =>
             * route("gsis.login"), "urlAuthorize" =>
             * env("GSIS_OAUTH2_URL_AUTHORIZE"),
             */
            Response challenge = context.form()
                    .setAttribute("clientId", propServ.getProp("TAXIS_CLIENT_ID"))
                    .setAttribute("redirectURI", propServ.getProp("TAXIS_REDIRECT_URI"))
                    .setAttribute("authorizeURI", propServ.getProp("TAXIS_OAUTH2_URL_AUTHORIZE"))
                    .setAttribute("state", sessionId)
                    .createForm("taxis.ftl");
            log.info("will respond with force challenge");
            context.forceChallenge(challenge);
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }
    }

    @Override
    public void actionImpl(AuthenticationFlowContext afc) {
        log.info("before eidas actionImp called");
    }

}
