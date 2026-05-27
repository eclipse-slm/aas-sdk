package org.eclipse.slm.aas.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class AasWebUiTestContainer extends GenericContainer<AasWebUiTestContainer> {

    public static final String SERVICE_ALIAS = "aas-web-ui";
    public static final int INTERNAL_PORT = 3000;
    public static final String IMAGE_NAME = "eclipsebasyx/aas-gui:SNAPSHOT";

    private final String externalUrlHost;
    private final int externalUrlPort;

    public AasWebUiTestContainer() {
        this(builder());
    }

    private AasWebUiTestContainer(Builder builder) {
        super(DockerImageName.parse(IMAGE_NAME));

        this.externalUrlHost = builder.externalUrlHost;
        this.externalUrlPort = builder.externalUrlPort;

        addFixedExposedPort(externalUrlPort, INTERNAL_PORT);
        withNetworkAliases(SERVICE_ALIAS);

        if (builder.aasRegistry != null) {
            withEnv("AAS_REGISTRY_PATH", builder.aasRegistry.getExternalUrl() + "/shell-descriptors");
        }

        if (builder.submodelRegistry != null) {
            withEnv("SUBMODEL_REGISTRY_PATH", builder.submodelRegistry.getExternalUrl() + "/submodel-descriptors");
        }

        if (builder.aasDiscovery != null) {
            withEnv("AAS_DISCOVERY_PATH", builder.aasDiscovery.getExternalUrl() + "/lookup/shells");
        }

        if (builder.aasEnvironment != null) {
            withEnv("AAS_REPO_PATH", builder.aasEnvironment.getExternalUrl() + "/shells");
            withEnv("SUBMODEL_REPO_PATH", builder.aasEnvironment.getExternalUrl() + "/submodels");
            withEnv("CD_REPO_PATH", builder.aasEnvironment.getExternalUrl() + "/concept-descriptions");
        }

        waitingFor(Wait.forListeningPort());
    }

    public String getExternalUrl() {
        return ExternalUrlSupport.buildUrl(externalUrlHost, externalUrlPort);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private AasRegistryTestContainer aasRegistry;
        private SubmodelRegistryTestContainer submodelRegistry;
        private AasDiscoveryTestContainer aasDiscovery;
        private AasEnvironmentTestContainer aasEnvironment;
        private String externalUrlHost = "localhost";
        private int externalUrlPort = ExternalUrlSupport.reserveFreePort();

        private Builder() {
            // Builder factory
        }

        public Builder withAasRegistry(AasRegistryTestContainer aasRegistry) {
            this.aasRegistry = aasRegistry;
            return this;
        }

        public Builder withSubmodelRegistry(SubmodelRegistryTestContainer submodelRegistry) {
            this.submodelRegistry = submodelRegistry;
            return this;
        }

        public Builder withAasDiscovery(AasDiscoveryTestContainer aasDiscovery) {
            this.aasDiscovery = aasDiscovery;
            return this;
        }

        public Builder withAasEnvironment(AasEnvironmentTestContainer aasEnvironment) {
            this.aasEnvironment = aasEnvironment;
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

        public AasWebUiTestContainer build() {
            return new AasWebUiTestContainer(this);
        }
    }
}
