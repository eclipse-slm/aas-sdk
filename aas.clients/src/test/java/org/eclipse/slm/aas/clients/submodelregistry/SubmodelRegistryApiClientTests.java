package org.eclipse.slm.aas.clients.submodelregistry;

import org.apache.logging.log4j.util.Base64Util;
import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEndpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;
import org.eclipse.slm.aas.clients.base.FeignClientFactory;
import org.eclipse.slm.aas.testcontainers.SubmodelRegistryTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
public class SubmodelRegistryApiClientTests {

    @Container
    static SubmodelRegistryTestContainer submodelRegistry = SubmodelRegistryTestContainer.builder().build();

    private SubmodelRegistryApiClient submodelRegistryApiClient;

    private Endpoint testSubmodelEndpoint = new DefaultEndpoint.Builder()
            ._interface("SUBMODEL-3.0")
            .protocolInformation(new DefaultProtocolInformation.Builder()
                    .endpointProtocol("http")
                    .href("localhost/testsubmodel")
                    .build()
            )
            .build();


    @BeforeEach
    public void setUp() {
        var registryUrl = submodelRegistry.getExternalUrl();
        this.submodelRegistryApiClient = FeignClientFactory.createClient(SubmodelRegistryApiClient.class, registryUrl, null);
    }

    @Nested
    @DisplayName("CRUD Submodel Descriptor")
    public class SubmodelDescriptorCrud {
        @Test
        @DisplayName("Should create and get Submodel Descriptor")
        public void shouldCreateAndGetSubmodelDescriptor() {
            var submodelId = UUID.randomUUID().toString();
            var submodelIdBase64Encoded = Base64Util.encode(submodelId);
            var submodelDescriptor = new DefaultSubmodelDescriptor.Builder()
                    .id(submodelId)
                    .idShort("submodel1")
                    .endpoints(testSubmodelEndpoint)
                    .build();

            submodelRegistryApiClient.postSubmodelDescriptor(submodelDescriptor);
            var fetched = submodelRegistryApiClient.getSubmodelDescriptorById(submodelIdBase64Encoded);
            assertThat(fetched).isNotNull();
            assertThat(fetched.getId()).isEqualTo(submodelId);
        }

        @Test
        @DisplayName("Should update Submodel Descriptor")
        public void shouldUpdateSubmodelDescriptor() {
            var submodelId = UUID.randomUUID().toString();
            var submodelIdBase64Encoded = Base64Util.encode(submodelId);
            var submodelDescriptor = new DefaultSubmodelDescriptor.Builder()
                    .id(submodelId)
                    .idShort("submodel1")
                    .endpoints(testSubmodelEndpoint)
                    .build();
            submodelRegistryApiClient.postSubmodelDescriptor(submodelDescriptor);

            var updatedDescriptor = new DefaultSubmodelDescriptor.Builder()
                    .id(submodelId)
                    .idShort("updatedSubmodel")
                    .endpoints(testSubmodelEndpoint)
                    .build();
            submodelRegistryApiClient.putSubmodelDescriptorById(submodelIdBase64Encoded, updatedDescriptor);
            var fetched = submodelRegistryApiClient.getSubmodelDescriptorById(submodelIdBase64Encoded);
            assertThat(fetched.getIdShort()).isEqualTo("updatedSubmodel");
        }

        @Test
        @DisplayName("Should delete Submodel Descriptor")
        public void shouldDeleteSubmodelDescriptor() {
            var submodelId = UUID.randomUUID().toString();
            var submodelIdBase64Encoded = Base64Util.encode(submodelId);
            var submodelDescriptor = new DefaultSubmodelDescriptor.Builder()
                    .id(submodelId)
                    .idShort("submodel1")
                    .endpoints(testSubmodelEndpoint)
                    .build();
            submodelRegistryApiClient.postSubmodelDescriptor(submodelDescriptor);
            submodelRegistryApiClient.deleteSubmodelDescriptorById(submodelIdBase64Encoded);
            assertThatThrownBy(() -> submodelRegistryApiClient.getSubmodelDescriptorById(submodelId)).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("GET all Submodel Descriptors")
    public class GetAllSubmodelDescriptors {
        @Test
        @DisplayName("Should return list of Submodel Descriptors")
        public void shouldReturnListOfSubmodelDescriptors() {
            var result = submodelRegistryApiClient.getAllSubmodelDescriptors(null, null);
            assertThat(result).isNotNull();
        }
    }
}
