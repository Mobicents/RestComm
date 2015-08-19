package org.mobicents.servlet.restcomm.identity;

import org.keycloak.adapters.HttpFacade.Request;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.mobicents.servlet.restcomm.identity.IdentityConfigurator.CloudIdentityNotSet;

public class RestcommConfKeycloakResolver implements KeycloakConfigResolver {

    //private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();
    private KeycloakDeployment cache;

    public RestcommConfKeycloakResolver() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public KeycloakDeployment resolve(Request request) {
        if ( cache == null) {
            IdentityConfigurator configurator = IdentityConfigurator.getInstance();
            try {
                cache = KeycloakDeploymentBuilder.build(configurator.getRestcommConfig());
            } catch (CloudIdentityNotSet e) {
                throw new IllegalStateException("No cloud identity set in restcomm.xml");
            }
        }
        return cache;
    }

}
