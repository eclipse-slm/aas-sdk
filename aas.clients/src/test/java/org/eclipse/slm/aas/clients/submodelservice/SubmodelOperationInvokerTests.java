package org.eclipse.slm.aas.clients.submodelservice;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEndpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;
import org.eclipse.slm.aas.clients.submodelrepository.SubmodelRepositoryClient;
import org.eclipse.slm.aas.clients.submodelrepository.SubmodelRepositoryClientFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SubmodelOperationInvokerTests {

    @Test
    void isSubmodelServiceEndpoint_detectsSubmodelServiceForm() {
        assertThat(SubmodelOperationInvoker.isSubmodelServiceEndpoint("http://host/submodel")).isTrue();
        assertThat(SubmodelOperationInvoker.isSubmodelServiceEndpoint("http://host/submodels/aGVsbG8")).isFalse();
    }

    @Test
    void invokeOperation_usesSubmodelServiceClient_forSubmodelServiceEndpoint() {
        var descriptor = descriptorWithEndpoint("submodel-1", "http://host:8081/submodel");
        var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        var submodelServiceClient = mock(SubmodelServiceClient.class);
        when(submodelServiceClient.invokeOperation("operations.doThing", input)).thenReturn(expectedOutput);

        try (MockedStatic<SubmodelServiceClient> mockedStatic = mockStatic(SubmodelServiceClient.class)) {
            mockedStatic.when(() -> SubmodelServiceClient.FromSubmodelDescriptor(descriptor, jwtAuthenticationToken))
                    .thenReturn(submodelServiceClient);

            var output = SubmodelOperationInvoker.invokeOperation(descriptor, jwtAuthenticationToken, "operations.doThing", input);

            assertThat(output).isSameAs(expectedOutput);
        }
    }

    @Test
    void invokeOperation_usesSubmodelRepositoryClient_forRepositoryEndpoint() {
        var descriptor = descriptorWithEndpoint("submodel-1", "http://host:8081/submodels/aGVsbG8");
        var jwtAuthenticationToken = mock(JwtAuthenticationToken.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        var submodelRepositoryClient = mock(SubmodelRepositoryClient.class);
        when(submodelRepositoryClient.invokeOperation("submodel-1", "operations.doThing", input)).thenReturn(expectedOutput);

        try (MockedStatic<SubmodelRepositoryClientFactory> mockedStatic = mockStatic(SubmodelRepositoryClientFactory.class)) {
            mockedStatic.when(() -> SubmodelRepositoryClientFactory.FromSubmodelDescriptor(descriptor, jwtAuthenticationToken))
                    .thenReturn(submodelRepositoryClient);

            var output = SubmodelOperationInvoker.invokeOperation(descriptor, jwtAuthenticationToken, "operations.doThing", input);

            assertThat(output).isSameAs(expectedOutput);
        }
    }

    private DefaultSubmodelDescriptor descriptorWithEndpoint(String submodelId, String href) {
        var endpoint = new DefaultEndpoint.Builder()
                ._interface("SUBMODEL-3.0")
                .protocolInformation(new DefaultProtocolInformation.Builder()
                        .endpointProtocol("HTTP")
                        .href(href)
                        .build())
                .build();

        return new DefaultSubmodelDescriptor.Builder()
                .id(submodelId)
                .idShort("testSubmodel")
                .endpoints(endpoint)
                .build();
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
