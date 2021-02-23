/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author nikos
 */
public class BeforeMinEduLDAPAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "before-mineduLDAP-authenticator";
    private static final BeforeMinEduLDAPAuthenticator SINGLETON = new BeforeMinEduLDAPAuthenticator();

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    private static AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.ALTERNATIVE,
        AuthenticationExecutionModel.Requirement.DISABLED,
        AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return true;
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getHelpText() {
        return "Placeholder Help Text.";
    }

    @Override
    public String getDisplayType() {
        return "before-mineduLDAP Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "before-mineduLDAP Authenticator";
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

}
