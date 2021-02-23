/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uaegean.pojo.MinEduLDAPResponse;
import gr.uaegean.pojo.VerifiableCredential;
import gr.uaegean.singleton.MemcacheSingleton;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

import static gr.uaegean.utils.CredentialsUtils.getClaimsFromVerifiedArray;

/**
 * @author nikos
 */
@Slf4j
public class AfterMinEduLDAPAuthenticator implements Authenticator {

    //    protected ParameterService paramServ = new ParameterServiceImpl();

    private ObjectMapper mapper;
    private MemcachedClient mcc;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            KeycloakSession session = context.getSession();
            RealmModel realm = context.getRealm();
            mapper = new ObjectMapper();

            String sessionId = context.getHttpRequest().getUri().getQueryParameters().getFirst("sessionId");
//            LOG.info(sessionId);
            if (StringUtils.isEmpty(sessionId)) {
                log.info("no  seessionId found!!!!!!! AfterMinEduLDAPAuthenticator");
                log.info("will continue with attempted");
                context.attempted();
                return;
            }

            this.mcc = MemcacheSingleton.getCache();
            String stringLDAPResponse = (String) this.mcc.get("mineduLDAP-" + String.valueOf(sessionId));
            log.info("GOT the following from cache {} ", stringLDAPResponse);
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            MinEduLDAPResponse response = mapper.readValue(stringLDAPResponse, MinEduLDAPResponse.class);
            // since we are not storing the users we use as a username the did
            UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, response.getUsername());
            if (user == null) {
                // since we are not storing the users we use as a username the did
                user = session.users().addUser(realm, response.getUsername());

            }
            user.setEnabled(true);
            user.setFirstName(response.getName());
            user.setLastName(response.getSurname());
            user.setUsername(response.getUsername());
            user.setEmail(response.getUsername() + "@mindedu");
            user.setEmailVerified(true);
            user.setSingleAttribute("username", response.getUsername());
            user.setSingleAttribute("name", response.getName());
            user.setSingleAttribute("surname", response.getSurname());
            user.setSingleAttribute("mail", response.getEmail());



            // grab oidc params
//            String response_type = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.RESPONSE_TYPE);
//            String client_id = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);
//            String redirect_uri = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
//            String state = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
//            String scope = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.SCOPE);

            context.setUser(user);
            log.info(" Success!! user is set {}", user.getUsername());
            context.success();
        } catch (IOException ex) {
            log.error(ex.getMessage());
            log.info("will continue with attempted");
            context.attempted();
        }
    }

    @Override
    public void action(AuthenticationFlowContext afc) {
        if (afc.getUser() != null) {
            afc.success();
        } else {
            afc.attempted();
        }
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

}
