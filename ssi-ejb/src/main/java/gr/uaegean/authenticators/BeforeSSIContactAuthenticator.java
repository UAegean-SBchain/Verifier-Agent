/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import gr.uaegean.pojo.KeycloakSessionTO;
import gr.uaegean.services.PropertiesService;
import gr.uaegean.singleton.MemcacheSingleton;
import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.core.Response;
import net.spy.memcached.MemcachedClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author nikos
 */
public class BeforeSSIContactAuthenticator extends AbstractSSIAuthenticator {

//    protected ParameterService paramServ = new ParameterServiceImpl();
    private static final Logger LOG = LoggerFactory.getLogger(BeforeSSIContactAuthenticator.class);
    private MemcachedClient mcc;
    private PropertiesService propServ;

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
            this.mcc = MemcacheSingleton.getCache();
            this.propServ = new PropertiesService();

            // grab oidc params
            String response_type = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.RESPONSE_TYPE);
            String client_id = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);
            String redirect_uri = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
            String state = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
            String scope = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.SCOPE);
            String realm = StringUtils.isEmpty(this.propServ.getProp("SSI_CONTACT_R")) ? "SSI-CONTACT" : this.propServ.getProp("SSI_CONTACT_R");
            String nonce = context.getHttpRequest().getUri().getQueryParameters().getFirst("nonce");
            String response_mode = context.getHttpRequest().getUri().getQueryParameters().getFirst("response_mode");
            int expiresInSec = 3000;
            // create a new sessionId
            String ssiSessionId = UUID.randomUUID().toString();
            //Transfer Object that will be cached
            KeycloakSessionTO ksTO = new KeycloakSessionTO(state, response_type, client_id, redirect_uri, state, scope, realm, nonce,response_mode);
            LOG.info("BeforeSSIPersonalAuth will cache with key: " + ssiSessionId + " object " + ksTO.toString());
            this.mcc.add(ssiSessionId, expiresInSec, ksTO);

            // GET the QR Code
            RestTemplate restTemplate = new RestTemplate();
            String uportHelperMsHost = StringUtils.isEmpty(this.propServ.getProp("UPORTHELPER")) ? "http://localhost:3000" : this.propServ.getProp("UPORTHELPER");
            String callback = StringUtils.isEmpty(this.propServ.getProp("CALLBACK")) ? "http://localhost:3000" : this.propServ.getProp("CALLBACK");
            String callbackMobile = StringUtils.isEmpty(this.propServ.getProp("CALLBACK_MOBILE")) ? "http://localhost:3000"
                    : this.propServ.getProp("CALLBACK_MOBILE");

            String resourceUrl = uportHelperMsHost + "/connectionRequest?" + "CONTACT_ME=true&" + "ssiSessionId=" + ssiSessionId + "&callback=" + callback;
            ResponseEntity<String> response
                    = restTemplate.getForEntity(resourceUrl.trim(), String.class);

            resourceUrl = uportHelperMsHost + "/connectionRequestMobile?" + "CONTACT_ME=true&" + "ssiSessionId=" + ssiSessionId
                    + "&callback=" + callbackMobile;

            ResponseEntity<String> responseMobile
                    = restTemplate.getForEntity(resourceUrl.trim(), String.class);

            String ssEventSource = this.propServ.getProp("EVENT_SOURCE");
            String responsePostEndpoint = this.propServ.getProp("SSI_REPLY_POST");

            Response challenge = context.form()
                    .setAttribute("qr", response.getBody())
                    .setAttribute("mobile", responseMobile.getBody())
                    .setAttribute("clientId", client_id)
                    .setAttribute("scopes", scope.split(" "))
                    .setAttribute("ssiSessionId", ssiSessionId)
                    .setAttribute("ssEventSource", ssEventSource)
                    .setAttribute("responsePostEndpoint", responsePostEndpoint)
                    .createForm("ssi-request.ftl");
            LOG.info("will respond with force challenge");
//force challenge means that it will not proceed to other authentication providers
            context.forceChallenge(challenge);
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    @Override
    public void actionImpl(AuthenticationFlowContext afc) {
        LOG.info("before eidas actionImp called");
    }

}
