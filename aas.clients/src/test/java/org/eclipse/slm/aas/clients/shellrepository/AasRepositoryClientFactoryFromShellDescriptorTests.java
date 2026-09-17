package org.eclipse.slm.aas.clients.shellrepository;

import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEndpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProtocolInformation;
import org.eclipse.digitaltwin.basyx.aasrepository.client.ConnectedAasRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AasRepositoryClientFactory#FromShellDescriptor(
 * org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor,
 * org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken)}.
 *
 * <p>These tests do not require a running container: they verify endpoint parsing,
 * token wiring, and error behaviour by inspecting the resulting
 * {@link AasRepositoryClient} through reflection to reach the underlying
 * {@link ConnectedAasRepository#getBaseUrl()}.
 */
class AasRepositoryClientFactoryFromShellDescriptorTests {

    // The href uses a trailing "/shells/<encoded-id>" segment so the regex
    // "(.*)/shells" captures the base URL.
    private static final String VALID_ENDPOINT_HREF =
            "http://foreign-host:8081/shells/aHR0cHM6Ly9leGFtcGxlLmNvbS9hYXMvMTIz";

    private DefaultAssetAdministrationShellDescriptor descriptorWithHref(String href) {
        ProtocolInformation proto = new DefaultProtocolInformation.Builder()
                .endpointProtocol("http")
                .href(href)
                .build();
        Endpoint endpoint = new DefaultEndpoint.Builder()
                ._interface("AAS-3.0")
                .protocolInformation(proto)
                .build();
        return new DefaultAssetAdministrationShellDescriptor.Builder()
                .id("urn:uuid:test-aas")
                .idShort("testShell")
                .endpoints(endpoint)
                .build();
    }

    /**
     * Extracts the base URL from an {@link AasRepositoryClient} by reaching through
     * to the wrapped {@link ConnectedAasRepository#getBaseUrl()}.
     */
    private String extractBaseUrl(AasRepositoryClient client) throws Exception {
        Field repoField = AasRepositoryClient.class.getDeclaredField("connectedAasRepository");
        repoField.setAccessible(true);
        ConnectedAasRepository connectedRepo = (ConnectedAasRepository) repoField.get(client);
        return connectedRepo.getBaseUrl();
    }

    // ── Happy path: valid endpoint ──────────────────────────────────────────

    @Test
    void fromShellDescriptor_withoutToken_shouldTargetDescriptorEndpoint() throws Exception {
        var descriptor = descriptorWithHref(VALID_ENDPOINT_HREF);

        var client = AasRepositoryClientFactory.FromShellDescriptor(descriptor);

        assertThat(client).isNotNull();
        assertThat(extractBaseUrl(client)).isEqualTo("http://foreign-host:8081");
    }

    @Test
    void fromShellDescriptor_withToken_shouldTargetDescriptorEndpoint() throws Exception {
        var descriptor = descriptorWithHref(VALID_ENDPOINT_HREF);
        var jwt = Mockito.mock(JwtAuthenticationToken.class);

        var client = AasRepositoryClientFactory.FromShellDescriptor(descriptor, jwt);

        assertThat(client).isNotNull();
        assertThat(extractBaseUrl(client)).isEqualTo("http://foreign-host:8081");
    }

    @Test
    void fromShellDescriptor_withNullToken_shouldBehaveLikeNoTokenVariant() throws Exception {
        var descriptor = descriptorWithHref(VALID_ENDPOINT_HREF);

        var clientNoArg = AasRepositoryClientFactory.FromShellDescriptor(descriptor);
        var clientNullToken = AasRepositoryClientFactory.FromShellDescriptor(descriptor, null);

        assertThat(extractBaseUrl(clientNoArg)).isEqualTo(extractBaseUrl(clientNullToken));
    }

    // ── Error path: invalid endpoint ───────────────────────────────────────

    @Test
    void fromShellDescriptor_invalidEndpointNoShellsSegment_shouldThrow() {
        var descriptor = descriptorWithHref("http://some-host:1234/invalid/path");

        assertThatThrownBy(() -> AasRepositoryClientFactory.FromShellDescriptor(descriptor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http://some-host:1234/invalid/path");
    }

    @Test
    void fromShellDescriptor_invalidEndpointWithToken_shouldThrow() {
        var descriptor = descriptorWithHref("http://some-host:1234/unknown");
        var jwt = Mockito.mock(JwtAuthenticationToken.class);

        assertThatThrownBy(() -> AasRepositoryClientFactory.FromShellDescriptor(descriptor, jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http://some-host:1234/unknown");
    }

    @Test
    void fromShellDescriptor_emptyEndpoints_shouldThrow() {
        var descriptor = new DefaultAssetAdministrationShellDescriptor.Builder()
                .id("urn:uuid:empty")
                .build();

        // No endpoints set => get(0) fails
        assertThatThrownBy(() -> AasRepositoryClientFactory.FromShellDescriptor(descriptor))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
