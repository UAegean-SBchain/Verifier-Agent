/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.authenticators;

import gr.uaegean.services.PropertiesService;
import java.io.IOException;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nikos
 */
public abstract class AbstractTaxisAuthenticator implements Authenticator {

//    protected ParameterService paramServ = new ParameterServiceImpl();
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTaxisAuthenticator.class);

    protected PropertiesService propServ;

    protected abstract void actionImpl(AuthenticationFlowContext context);

    protected abstract void authenticateImpl(AuthenticationFlowContext context);

    public void initServices() throws IOException {
        LOG.info("AbstractTaxisAuthenticator:: will inti properites");
        this.propServ = new PropertiesService();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        actionImpl(context);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        try {
            initServices();
            authenticateImpl(context);
        } catch (IOException ex) {
            LOG.error("error reading properties");
            LOG.error(ex.getMessage());
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

}
