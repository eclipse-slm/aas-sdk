package org.eclipse.slm.aas.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class AasRegistryTestContainer extends GenericContainer<AasRegistryTestContainer> {

    public static final String SERVICE_ALIAS = "aas-registry";
    public static final int INTERNAL_PORT = 8082;
    public static final String IMAGE_NAME = "eclipsebasyx/aas-registry-log-mem:2.0.0-SNAPSHOT";

    private final String externalUrlHost;
    private final int externalUrlPort;

    public AasRegistryTestContainer() {
        this(builder());
    }

    private AasRegistryTestContainer(Builder builder) {
        super(DockerImageName.parse(IMAGE_NAME));

        this.externalUrlHost = builder.externalUrlHost;
        this.externalUrlPort = builder.externalUrlPort;

        withNetworkAliases(SERVICE_ALIAS);
        addFixedExposedPort(externalUrlPort, INTERNAL_PORT);
        withEnv("SERVER_PORT", String.valueOf(INTERNAL_PORT));
        withEnv("BASYX_CORS_ALLOWED_ORIGINS", "*");
        withEnv("BASYX_CORS_ALLOWED_METHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD");
        withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "health,info");
        withEnv("MANAGEMENT_INFO_GIT_ENABLED", "false");
        waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));
    }

    public String getExternalUrl() {
        return ExternalUrlSupport.buildUrl(externalUrlHost, externalUrlPort);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String externalUrlHost = "localhost";
        private int externalUrlPort = ExternalUrlSupport.reserveFreePort();

        private Builder() {
            // Builder factory
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

        public AasRegistryTestContainer build() {
            return new AasRegistryTestContainer(this);
        }
    }
}
