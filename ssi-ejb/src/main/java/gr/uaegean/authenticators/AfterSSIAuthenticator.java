/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uaegean.pojo.VerifiableCredential;
import gr.uaegean.pojo.VerifiableCredential.VerifiedClaims;
import gr.uaegean.singleton.MemcacheSingleton;
import static gr.uaegean.utils.CredentialsUtils.getClaimsFromVerifiedArray;
import java.io.IOException;
import net.spy.memcached.MemcachedClient;
import org.apache.commons.codec.digest.DigestUtils;
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
 *
 * @author nikos
 */
public class AfterSSIAuthenticator implements Authenticator {

//    protected ParameterService paramServ = new ParameterServiceImpl();
    private static Logger LOG = LoggerFactory.getLogger(AfterSSIAuthenticator.class);

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
            VerifiedClaims vc = getClaimsFromVerifiedArray(credential);

            // since we are not storing the users we use as a username the did
            UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm,
                    DigestUtils.sha256Hex(credential.toString()));
            if (user == null) {
                // since we are not storing the users we use as a username the did
                user = session.users().addUser(realm, DigestUtils.sha256Hex(credential.toString()));
                user.setEnabled(true);
            }


            //TODO Add other data sources!!!
            if (vc.getEidasEid() != null && vc.getEidasEid().getAttributes() != null) {
                user.setFirstName(vc.getEidasEid().getAttributes().getFirstName());
                user.setLastName(vc.getEidasEid().getAttributes().getLastName());
                user.setEmail(DigestUtils.sha256Hex(vc.toString()) + "@uport");
                user.setEmailVerified(true);

                user.setSingleAttribute("eidas-familyName", vc.getEidasEid().getAttributes().getLastName());
                user.setSingleAttribute("eidas-firstName", vc.getEidasEid().getAttributes().getFirstName());
                user.setSingleAttribute("eidas-dateOfBirth", vc.getEidasEid().getAttributes().getDateOfBirth());
                user.setSingleAttribute("eidas-loa", vc.getEidasEid().getAttributes().getLoa());
                user.setSingleAttribute("eidas-credential-id", vc.getId());
            }


            if (vc.getTaxisId() != null && vc.getTaxisId().getClaims() != null) {
                if(StringUtils.isEmpty(user.getFirstName()))user.setFirstName(vc.getTaxisId().getClaims().getFirstName());
                if(StringUtils.isEmpty(user.getLastName()))user.setLastName(vc.getTaxisId().getClaims().getLastName());
                if(StringUtils.isEmpty(user.getEmail()))user.setEmail(DigestUtils.sha256Hex(vc.toString())+ "@uport");
                user.setSingleAttribute("taxis-familyName", vc.getTaxisId().getClaims().getLastName());
                user.setSingleAttribute("taxis-firstName", vc.getTaxisId().getClaims().getFirstName());
                user.setSingleAttribute("taxis-afm", vc.getTaxisId().getClaims().getAfm());
                user.setSingleAttribute("taxis-fathersName", vc.getTaxisId().getClaims().getFathersName());
                user.setSingleAttribute("taxis-mothersName", vc.getTaxisId().getClaims().getMothersName());
                user.setSingleAttribute("taxis-source", vc.getTaxisId().getMetadata().getSource());
                user.setSingleAttribute("taxis-dateOfBirth", vc.getTaxisId().getClaims().getYearOfBirth());
                user.setSingleAttribute("taxis-credential-id", vc.getTaxisId().getMetadata().getId());
                user.setSingleAttribute("iat", credential.getVerified()[0].getIat());
                user.setSingleAttribute("exp", credential.getVerified()[0].getExp());
                user.setSingleAttribute("credential-name", "Taxis");

            }



            if (vc.getSealEidas() != null && vc.getSealEidas().getEidas() != null) {
                user.setFirstName(vc.getSealEidas().getEidas().getGivenName());
                user.setLastName(vc.getSealEidas().getEidas().getFamilyName());
                user.setEmail(vc.getSealEidas().getEidas().getPersonIdentifier() + "@uport");
                user.setEmailVerified(true);

                user.setSingleAttribute("eidas-familyName", vc.getSealEidas().getEidas().getFamilyName());
                user.setSingleAttribute("eidas-firstName", vc.getSealEidas().getEidas().getGivenName());
                user.setSingleAttribute("eidas-dateOfBirth", vc.getSealEidas().getEidas().getDateOfBirth());
                user.setSingleAttribute("eidas-personIdentifier", vc.getSealEidas().getEidas().getPersonIdentifier());
                user.setSingleAttribute("eidas-loa", vc.getSealEidas().getEidas().getLoa());
                user.setSingleAttribute("eidas-credential-id", vc.getId());

            }

            if (vc.getEidasEdugain() != null && vc.getEidasEdugain().getEidas() != null
                    && vc.getEidasEdugain().getEdugain() != null) {
                user.setFirstName(vc.getEidasEdugain().getEidas().getGivenName());
                user.setLastName(vc.getEidasEdugain().getEidas().getFamilyName());
                user.setEmail(vc.getEidasEdugain().getEidas().getPersonIdentifier() + "@eidas-edugain-uport");
                user.setEmailVerified(true);

                user.setSingleAttribute("eidas-familyName", vc.getEidasEdugain().getEidas().getFamilyName());
                user.setSingleAttribute("eidas-firstName", vc.getEidasEdugain().getEidas().getGivenName());
                user.setSingleAttribute("eidas-dateOfBirth", vc.getEidasEdugain().getEidas().getDateOfBirth());
                user.setSingleAttribute("eidas-personIdentifier", vc.getEidasEdugain().getEidas().getPersonIdentifier());
                user.setSingleAttribute("eidas-loa", vc.getEidasEdugain().getEidas().getLoa());
                user.setSingleAttribute("eidas-credential-id", vc.getId());

                user.setSingleAttribute("edugain-mail", vc.getEidasEdugain().getEdugain().getMail());
                user.setSingleAttribute("edugain-given_name", vc.getEidasEdugain().getEdugain().getGivenName());
                user.setSingleAttribute("edugain-sn", vc.getEidasEdugain().getEdugain().getSn());
                user.setSingleAttribute("edugain-display_name", vc.getEidasEdugain().getEdugain().getDisplayName());
                user.setSingleAttribute("edugain-edu_person_entitlement", vc.getEidasEdugain().getEdugain().getEduPersonEntitlemenet());
                user.setSingleAttribute("edugain-source", vc.getEidasEdugain().getEdugain().getSource());
                user.setSingleAttribute("edugain-loa", vc.getEidasEdugain().getEdugain().getLoa());

                user.setSingleAttribute("link-loa", vc.getEidasEdugain().getLinkLoa());

            }

            if (vc.getErasmus() != null && vc.getErasmus().getMitro() != null) {
                user.setSingleAttribute("eidas-familyName", vc.getErasmus().getMitro().getFamilyName());
                user.setSingleAttribute("eidas-firstName", vc.getErasmus().getMitro().getGivenName());
                user.setSingleAttribute("eidas-dateOfBirth", vc.getErasmus().getMitro().getDateOfBirth());
                user.setSingleAttribute("eidas-personIdentifier", vc.getErasmus().getMitro().getPersonIdentifier());
                user.setSingleAttribute("eidas-loa", vc.getErasmus().getMitro().getLoa());
                user.setSingleAttribute("erasmus-expires", vc.getErasmus().getMitro().getExpires());
                user.setSingleAttribute("erasmus-hostingInstitution", vc.getErasmus().getMitro().getHostingInstitution());
                user.setSingleAttribute("erasmus-affiliation", vc.getErasmus().getMitro().getAffiliation());
            }


            // grab oidc params
            String response_type = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.RESPONSE_TYPE);
            String client_id = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.CLIENT_ID);
            String redirect_uri = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.REDIRECT_URI);
            String state = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.STATE);
            String scope = context.getHttpRequest().getUri().getQueryParameters().getFirst(OAuth2Constants.SCOPE);
            //DEbug ensure we are getting the correct parameaters here
            LOG.info("AFTER SSI Authenticator parameters!!!");
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
