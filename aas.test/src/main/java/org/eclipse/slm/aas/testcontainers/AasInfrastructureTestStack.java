package org.eclipse.slm.aas.testcontainers;

import org.testcontainers.containers.Network;

public class AasInfrastructureTestStack implements AutoCloseable {

    private final Network network;
    private final AasRegistryTestContainer aasRegistry;
    private final SubmodelRegistryTestContainer submodelRegistry;
    private final AasDiscoveryTestContainer aasDiscovery;
    private final AasEnvironmentTestContainer aasEnvironment;
    private final AasWebUiTestContainer aasWebUi;

    private volatile boolean started;

    public AasInfrastructureTestStack() {
        this(builder());
    }

    private AasInfrastructureTestStack(Builder builder) {
        network = Network.newNetwork();

        aasRegistry = AasRegistryTestContainer.builder()
                .withExternalUrlHost(builder.externalUrlHost)
                .withExternalUrlPort(builder.aasRegistryExternalUrlPort)
                .build()
                .withNetwork(network)
                .withNetworkAliases(AasRegistryTestContainer.SERVICE_ALIAS);

        submodelRegistry = SubmodelRegistryTestContainer.builder()
                .withExternalUrlHost(builder.externalUrlHost)
                .withExternalUrlPort(builder.submodelRegistryExternalUrlPort)
                .build()
                .withNetwork(network)
                .withNetworkAliases(SubmodelRegistryTestContainer.SERVICE_ALIAS);

        aasDiscovery = AasDiscoveryTestContainer.builder()
                .withExternalUrlHost(builder.externalUrlHost)
                .withExternalUrlPort(builder.aasDiscoveryExternalUrlPort)
                .build()
                .withNetwork(network)
                .withNetworkAliases(AasDiscoveryTestContainer.SERVICE_ALIAS);

        aasEnvironment = AasEnvironmentTestContainer.builder()
                .withAasRegistry(aasRegistry)
                .withAasDiscovery(aasDiscovery)
                .withSubmodelRegistry(submodelRegistry)
                .withExternalUrlHost(builder.externalUrlHost)
                .withExternalUrlPort(builder.aasEnvironmentExternalUrlPort)
                .build()
                .withNetwork(network)
                .withNetworkAliases(AasEnvironmentTestContainer.SERVICE_ALIAS)
                .dependsOn(aasRegistry, submodelRegistry, aasDiscovery);

        aasWebUi = AasWebUiTestContainer.builder()
                .withAasRegistry(aasRegistry)
                .withSubmodelRegistry(submodelRegistry)
                .withAasDiscovery(aasDiscovery)
                .withAasEnvironment(aasEnvironment)
                .withExternalUrlHost(builder.externalUrlHost)
                .withExternalUrlPort(builder.aasWebUiExternalUrlPort)
                .build()
                .withNetwork(network)
                .withNetworkAliases(AasWebUiTestContainer.SERVICE_ALIAS)
                .dependsOn(aasEnvironment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        aasRegistry.start();
        submodelRegistry.start();
        aasDiscovery.start();
        aasEnvironment.start();
        aasWebUi.start();

        started = true;
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }

        aasWebUi.stop();
        aasEnvironment.stop();
        aasDiscovery.stop();
        submodelRegistry.stop();
        aasRegistry.stop();

        network.close();
        started = false;
    }

    @Override
    public void close() {
        stop();
    }

    public String getAasEnvironmentBaseUrl() {
        return aasEnvironment.getExternalUrl();
    }

    public String getAasRegistryBaseUrl() {
        return aasRegistry.getExternalUrl();
    }

    public String getSubmodelRegistryBaseUrl() {
        return submodelRegistry.getExternalUrl();
    }

    public String getAasDiscoveryBaseUrl() {
        return aasDiscovery.getExternalUrl();
    }

    public String getAasWebUiBaseUrl() {
        return aasWebUi.getExternalUrl();
    }

    public static final class Builder {
        private String externalUrlHost = "localhost";
        private int aasRegistryExternalUrlPort = ExternalUrlSupport.reserveFreePort();
        private int submodelRegistryExternalUrlPort = ExternalUrlSupport.reserveFreePort();
        private int aasDiscoveryExternalUrlPort = ExternalUrlSupport.reserveFreePort();
        private int aasEnvironmentExternalUrlPort = ExternalUrlSupport.reserveFreePort();
        private int aasWebUiExternalUrlPort = ExternalUrlSupport.reserveFreePort();

        private Builder() {
            // Builder factory
        }

        public Builder withExternalUrlHost(String externalUrlHost) {
            this.externalUrlHost = ExternalUrlSupport.normalizeHost(externalUrlHost);
            return this;
        }

        public Builder withAasRegistryExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.aasRegistryExternalUrlPort = externalUrlPort;
            return this;
        }

        public Builder withSubmodelRegistryExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.submodelRegistryExternalUrlPort = externalUrlPort;
            return this;
        }

        public Builder withAasDiscoveryExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.aasDiscoveryExternalUrlPort = externalUrlPort;
            return this;
        }

        public Builder withAasEnvironmentExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.aasEnvironmentExternalUrlPort = externalUrlPort;
            return this;
        }

        public Builder withAasWebUiExternalUrlPort(int externalUrlPort) {
            ExternalUrlSupport.validatePort(externalUrlPort);
            this.aasWebUiExternalUrlPort = externalUrlPort;
            return this;
        }

        public AasInfrastructureTestStack build() {
            return new AasInfrastructureTestStack(this);
        }
    }
}
