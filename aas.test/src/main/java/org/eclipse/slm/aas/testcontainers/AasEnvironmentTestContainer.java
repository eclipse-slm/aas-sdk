package org.eclipse.slm.aas.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class AasEnvironmentTestContainer extends GenericContainer<AasEnvironmentTestContainer> {

    public static final String SERVICE_ALIAS = "aas-env";
    public static final int INTERNAL_PORT = 8081;
    public static final String IMAGE_NAME = "eclipsebasyx/aas-environment:2.0.0-SNAPSHOT";

    private final String externalUrlHost;
    private final int externalUrlPort;

    private AasEnvironmentTestContainer(Builder builder) {
        super(DockerImageName.parse(IMAGE_NAME));

        this.externalUrlHost = builder.externalUrlHost;
        this.externalUrlPort = builder.externalUrlPort;

        withNetworkAliases(SERVICE_ALIAS);
        addFixedExposedPort(externalUrlPort, INTERNAL_PORT);
        withEnv("SERVER_PORT", String.valueOf(INTERNAL_PORT));
        withEnv("BASYX_AASREPOSITORY_FEATURE_MQTT_ENABLED", "false");
        withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_MQTT_ENABLED", "false");
        withEnv("BASYX_CORS_ALLOWED_ORIGINS", "*");
        withEnv("BASYX_CORS_ALLOWED_METHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD");
        withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health,info");
        withEnv("MANAGEMENT_INFO_GIT_ENABLED", "false");

        if (builder.aasRegistry != null) {
            withEnv("BASYX_AASREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://" + AasRegistryTestContainer.SERVICE_ALIAS + ":" + AasRegistryTestContainer.INTERNAL_PORT);
        }

        if (builder.aasDiscovery != null) {
            withEnv("BASYX_AASREPOSITORY_FEATURE_DISCOVERYINTEGRATION", "http://" + AasDiscoveryTestContainer.SERVICE_ALIAS + ":" + AasDiscoveryTestContainer.INTERNAL_PORT);
        }

        if (builder.submodelRegistry != null) {
            withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://" + SubmodelRegistryTestContainer.SERVICE_ALIAS + ":" + SubmodelRegistryTestContainer.INTERNAL_PORT);
        }

        withEnv("BASYX_EXTERNALURL", getExternalUrl());
        withEnv("BASYX_BACKEND", "InMemory");
        waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));
    }

    public String getExternalUrl() {
        return ExternalUrlSupport.buildUrl(externalUrlHost, externalUrlPort);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private AasRegistryTestContainer aasRegistry;
        private AasDiscoveryTestContainer aasDiscovery;
        private SubmodelRegistryTestContainer submodelRegistry;
        private String externalUrlHost = "localhost";
        private int externalUrlPort = ExternalUrlSupport.reserveFreePort();

        private Builder() {
            // Builder factory
        }

        public Builder withAasRegistry(AasRegistryTestContainer aasRegistry) {
            this.aasRegistry = aasRegistry;
            return this;
        }

        public Builder withAasDiscovery(AasDiscoveryTestContainer aasDiscovery) {
            this.aasDiscovery = aasDiscovery;
            return this;
        }

        public Builder withSubmodelRegistry(SubmodelRegistryTestContainer submodelRegistry) {
            this.submodelRegistry = submodelRegistry;
            return this;
        }

        public Builder withExternalUrlHost(String externalUrlHost) {
            this.externalUrlHost = ExternalUrlSupport.normalizeHost(externalUrlHost);
            return this;
        }

        public Builder withExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.externalUrlPort = externalUrlPort;
            return this;
        }

        public AasEnvironmentTestContainer build() {
            return new AasEnvironmentTestContainer(this);
        }
    }
}
