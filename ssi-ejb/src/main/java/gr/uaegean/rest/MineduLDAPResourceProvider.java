package gr.uaegean.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class MineduLDAPResourceProvider implements RealmResourceProvider {

	private KeycloakSession session;

	public MineduLDAPResourceProvider(KeycloakSession session) {
        this.session = session;
    }

	@Override
	public Object getResource() {
		return new MineduLDAPRestResource(session);
	}
	
	@Override
	public void close() {
	}

}
