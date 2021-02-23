package gr.uaegean.rest;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class SsiSpResourceProviderFactory implements RealmResourceProviderFactory {

    // this becomes part of the path of the endoints
    // e.g. https://dss1.aegean.gr/auth/realms/test/ssi-sp/metadata
    public static final String ID = "ssi-sp";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new SsiSpResourceProvider(session);
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
