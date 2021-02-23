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
public class AfterSSIFinancialAuthenticator implements Authenticator {

    //    protected ParameterService paramServ = new ParameterServiceImpl();
    private static Logger LOG = LoggerFactory.getLogger(AfterSSIFinancialAuthenticator.class);

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

            if (vc.getFStatus() != null) {
                user.setFirstName(vc.getFStatus().getClaims().getName());
                user.setLastName(vc.getFStatus().getClaims().getSurname());
                user.setEmail(vc.getFStatus().getClaims().getAfm() + "@uport");
                user.setEmailVerified(true);
                user.setSingleAttribute("e1-salaries", vc.getFStatus().getClaims().getSalaryRevenues());
                user.setSingleAttribute("e1-pensionIncome", vc.getFStatus().getClaims().getPensionRevenues());
                user.setSingleAttribute("e1-farmingActivity", vc.getFStatus().getClaims().getFarmingActivity());
                user.setSingleAttribute("e1-freelanceActivity", vc.getFStatus().getClaims().getFreelanceActivity());
                user.setSingleAttribute("e1-rentIncome", vc.getFStatus().getClaims().getRentIncome());
                user.setSingleAttribute("e1-unemploymentBenefit", vc.getFStatus().getClaims().getUnemploymentBenefit());
                user.setSingleAttribute("e1-otherBenefitsIncome", vc.getFStatus().getClaims().getOtherBenefitsIncome());
                user.setSingleAttribute("e1-ekas", vc.getFStatus().getClaims().getEkas());
                user.setSingleAttribute("e1-additionalIncomes", vc.getFStatus().getClaims().getAdditionalIncomes());
                user.setSingleAttribute("e1-ergome", vc.getFStatus().getClaims().getErgosimo());
                user.setSingleAttribute("e1-depositInterest", vc.getFStatus().getClaims().getIntrestsProperty());
                user.setSingleAttribute("e1-deposits", vc.getFStatus().getClaims().getDeposits());
                user.setSingleAttribute("e1-valueOfRealEstate", vc.getFStatus().getClaims().getRealEstateValueDomestic());
                user.setSingleAttribute("e1-valueOfRealEstateInOtherCountries", vc.getFStatus().getClaims().getRealEstateValueOffshore());
                user.setSingleAttribute("e1-valueOfOwnedVehicles", vc.getFStatus().getClaims().getValueOfOwnedVehicles());
                user.setSingleAttribute("e1-investments", vc.getFStatus().getClaims().getInvestments());
                user.setSingleAttribute("e1-householdComposition", vc.getFStatus().getClaims().getHouseholdComposition());
                user.setSingleAttribute("e1-name", vc.getFStatus().getClaims().getName());
                user.setSingleAttribute("e1-surname", vc.getFStatus().getClaims().getSurname());
                user.setSingleAttribute("e1-credential-id", vc.getFStatus().getMetadata().getId());
                user.setSingleAttribute("iat", credential.getVerified()[0].getIat());
                user.setSingleAttribute("exp", credential.getVerified()[0].getExp());
                user.setSingleAttribute("credential-name", "Financial Information");
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
