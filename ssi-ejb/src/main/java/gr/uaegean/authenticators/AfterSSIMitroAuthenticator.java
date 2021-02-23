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
public class AfterSSIMitroAuthenticator implements Authenticator {

    //    protected ParameterService paramServ = new ParameterServiceImpl();
    private static Logger LOG = LoggerFactory.getLogger(AfterSSIMitroAuthenticator.class);

    private ObjectMapper mapper;
    private MemcachedClient mcc;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            KeycloakSession session = context.getSession();
            RealmModel realm = context.getRealm();
            mapper = new ObjectMapper();

            LOG.info("reached after-SSI-authenticator!!!!!");

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
//            LOG.info("got mitro parased to ");
//            LOG.info(vc.getMitro().toString());
            if (vc.getCivilRegistryId() != null) {
                user.setEmail(credential.getDid() + "@uport");

                user.setSingleAttribute("mitro-parenthood", vc.getCivilRegistryId().getClaims().getParenthood());
                user.setSingleAttribute("mitro-name", vc.getCivilRegistryId().getClaims().getName());
                user.setSingleAttribute("mitro-surname", vc.getCivilRegistryId().getClaims().getSurname());
                user.setSingleAttribute("nameLatin", vc.getCivilRegistryId().getClaims().getNameLatin());
                user.setSingleAttribute("surnameLatin", vc.getCivilRegistryId().getClaims().getSurnameLatin());
                user.setSingleAttribute("fatherName", vc.getCivilRegistryId().getClaims().getFatherName());
                user.setSingleAttribute("fatherLatin", vc.getCivilRegistryId().getClaims().getFatherNameLatin());
                user.setSingleAttribute("motherName", vc.getCivilRegistryId().getClaims().getMotherName());
                user.setSingleAttribute("motherLatin", vc.getCivilRegistryId().getClaims().getMotherNameLatin());
                user.setSingleAttribute("dateOfBirth", vc.getCivilRegistryId().getClaims().getBirthDate());
                user.setSingleAttribute("singleParet", vc.getCivilRegistryId().getClaims().getSingleParent());
                user.setSingleAttribute("numberOfChildren",vc.getCivilRegistryId().getClaims().getNumberOfChildren());
                user.setSingleAttribute("childrenIdentities",vc.getCivilRegistryId().getClaims().getChildrenIdentity());
                user.setSingleAttribute("mitro-custody", vc.getCivilRegistryId().getClaims().getCustody());
                user.setSingleAttribute("mitro-additionalAdults", vc.getCivilRegistryId().getClaims().getNumberOfChildren());
                user.setSingleAttribute("amka", vc.getCivilRegistryId().getClaims().getAmka());
                user.setSingleAttribute("nationality", vc.getCivilRegistryId().getClaims().getNationality());
                user.setSingleAttribute("maritalStatus", vc.getCivilRegistryId().getClaims().getMaritalStatus());
                user.setSingleAttribute("iat", credential.getVerified()[0].getIat());
                user.setSingleAttribute("exp", credential.getVerified()[0].getExp());
                user.setSingleAttribute("credential-name", "CIVIL_ID");
                if (vc.getCivilRegistryId().getClaims().getGender().equals("Άρρεν")) {
                    user.setSingleAttribute("gender", "male");
                } else {
                    user.setSingleAttribute("gender", "female");
                }
                user.setSingleAttribute("mitro-credential-id", vc.getCivilRegistryId().getMetadata().getId());


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
