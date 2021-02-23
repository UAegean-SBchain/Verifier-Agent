/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uaegean.pojo.VerifiableCredential;
import gr.uaegean.singleton.MemcacheSingleton;

import static gr.uaegean.utils.CredentialsUtils.getClaimsFromVerifiedArray;

import java.io.IOException;

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

/**
 * @author nikos
 */
public class AfterSSIEbillAuthenticator implements Authenticator {

    //    protected ParameterService paramServ = new ParameterServiceImpl();
    private static Logger LOG = LoggerFactory.getLogger(AfterSSIEbillAuthenticator.class);

    private ObjectMapper mapper;
    private MemcachedClient mcc;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            KeycloakSession session = context.getSession();
            RealmModel realm = context.getRealm();
            mapper = new ObjectMapper();

            LOG.info("reached after-SSI-TAXIS-authenticator!!!!!");

            String sessionId = context.getHttpRequest().getUri().getQueryParameters().getFirst("sessionId");
            LOG.info(sessionId);
            if (StringUtils.isEmpty(sessionId)) {
                LOG.info("no  seessionId found!!!!!!! AFTERSSIAuthenticator");
                LOG.info("will continue with attempted");
                context.attempted();
                return;
            }

            this.mcc = MemcacheSingleton.getCache();
            LOG.info("looking for: " + "claims" + String.valueOf(sessionId));
            String claims = (String) this.mcc.get("claims" + String.valueOf(sessionId));
            LOG.info("GOT the following SSI claims " + claims);

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            VerifiableCredential credential = mapper.readValue(claims, VerifiableCredential.class);
            VerifiableCredential.VerifiedClaims vc = getClaimsFromVerifiedArray(credential);

            // since we are not storing the users we use as a username the did
            UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, credential.getDid());
            if (user == null) {
                // since we are not storing the users we use as a username the did
                user = session.users().addUser(realm, credential.getDid());
            }
            user.setEnabled(true);

            if (vc.getEBillId() != null) {
                if (user.getFirstName() == null) {
                    user.setFirstName(vc.getEBillId().getClaims().getName());
                }
                if (user.getLastName() == null) {
                    user.setLastName(vc.getEBillId().getClaims().getSurname());
                }
                if (user.getEmail() == null) {
                    user.setEmail(vc.getEBillId().getClaims().getAfm() + "-ebill" + "@uport");
                }
                user.setEmailVerified(true);
                user.setSingleAttribute("meterNumber", vc.getEBillId().getClaims().getMeterNumber());
                user.setSingleAttribute("supplyType", vc.getEBillId().getClaims().getSupplyType());
                user.setSingleAttribute("ownership", vc.getEBillId().getClaims().getOwnership());
                user.setSingleAttribute("ebill-credential-id", vc.getEBillId().getMetadata().getId());
                user.setSingleAttribute("iat", credential.getVerified()[0].getIat());
                user.setSingleAttribute("exp", credential.getVerified()[0].getExp());
                user.setSingleAttribute("credential-name", "Electronic_Bill_ID");

                user.setSingleAttribute("ebill-name", credential.getEBillId().getClaims().getName());
                user.setSingleAttribute("ebill-surname", credential.getEBillId().getClaims().getSurname());
                user.setSingleAttribute("ebill-fatherName", credential.getEBillId().getClaims().getFatherName());
                user.setSingleAttribute("ebill-afm", credential.getEBillId().getClaims().getAfm());
                user.setSingleAttribute("ebill-street", credential.getEBillId().getClaims().getAddress().getStreet());
                user.setSingleAttribute("ebill-municipality", credential.getEBillId().getClaims().getAddress().getMunicipality());
                user.setSingleAttribute("ebill-number", credential.getEBillId().getClaims().getAddress().getNumber());
                user.setSingleAttribute("ebill-po", credential.getEBillId().getClaims().getAddress().getPo());
                user.setSingleAttribute("ebill-endorser", credential.getEBillId().getMetadata().getEndorser());
                user.setSingleAttribute("ebill-verification", credential.getEBillId().getMetadata().getVerification());


            }


            // grab oidc params
            String response_type = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.RESPONSE_TYPE);
            String client_id = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);
            String redirect_uri = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
            String state = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
            String scope = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.SCOPE);
            //DEbug ensure we are getting the correct parameaters here
            LOG.info("AFTER SSI PERSONAL Authenticator parameters!!!");
            LOG.info(response_type);
            LOG.info(client_id);
            LOG.info(redirect_uri);
            LOG.info(state);
            LOG.info(scope);

            context.setUser(user);
            LOG.info("AfterSSIAuthenticator Success!! user is set " + user.getUsername());

            context.success();
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
            LOG.info("will continue with attempted");
            context.attempted();
        }
    }

    @Override
    public void action(AuthenticationFlowContext afc) {
        LOG.info("AFTER eidas actionImp called");
//        LOG.info(afc.getUser());
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
