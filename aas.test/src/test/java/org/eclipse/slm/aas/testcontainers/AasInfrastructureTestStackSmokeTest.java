package org.eclipse.slm.aas.testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

class AasInfrastructureTestStackSmokeTest {

    @Test
    @EnabledIfSystemProperty(named = "runAasStackTest", matches = "true")
    void shouldStartStackWhenDockerFolderContainsAllRequiredFiles() {
        try (var stack = new AasInfrastructureTestStack()) {
            stack.start();

            var aasEnvUrl = stack.getAasEnvironmentBaseUrl();
            var aasRegistryUrl = stack.getAasRegistryBaseUrl();
            var aasDiscoveryUrl = stack.getAasDiscoveryBaseUrl();
            var submodelRegistryUrl = stack.getSubmodelRegistryBaseUrl();
            var aasWebUiUrl = stack.getAasWebUiBaseUrl();

            assertThat(stack.getAasEnvironmentBaseUrl()).startsWith("http://");
        }
    }
}

