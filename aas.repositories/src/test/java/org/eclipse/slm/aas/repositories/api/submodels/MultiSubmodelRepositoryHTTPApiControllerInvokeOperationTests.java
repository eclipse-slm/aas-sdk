package org.eclipse.slm.aas.repositories.api.submodels;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationRequest;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncodedIdentifier;
import org.eclipse.slm.aas.repositories.exceptions.MethodNotImplementedException;
import org.eclipse.slm.aas.repositories.exceptions.SubmodelNotFoundException;
import org.eclipse.slm.aas.repositories.submodels.SubmodelRepository;
import org.eclipse.slm.aas.repositories.submodels.SubmodelRepositoryFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiSubmodelRepositoryHTTPApiControllerInvokeOperationTests {

    private final Base64UrlEncodedIdentifier aasId = new Base64UrlEncodedIdentifier("aas-1");
    private final Base64UrlEncodedIdentifier submodelId = new Base64UrlEncodedIdentifier("deployment-42");

    @Test
    void invokeOperationSubmodelRepo_returnsOperationResult_withOutputArguments() {
        var submodelRepository = mock(SubmodelRepository.class);
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(submodelRepository.invokeOperation(eq("deployment-42"), eq("operations.doThing"), any(OperationVariable[].class)))
                .thenReturn(expectedOutput);

        var factory = mock(SubmodelRepositoryFactory.class);
        when(factory.getSubmodelRepository("aas-1")).thenReturn(submodelRepository);

        var controller = new MultiSubmodelRepositoryHTTPApiController(factory) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        var response = controller.invokeOperationSubmodelRepo(aasId, submodelId, "operations.doThing", body, false);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getOutputArguments()).containsExactly(expectedOutput);
    }

    @Test
    void invokeOperationSubmodelRepo_throwsMethodNotImplementedException_whenAsyncTrue() {
        var factory = mock(SubmodelRepositoryFactory.class);
        var controller = new MultiSubmodelRepositoryHTTPApiController(factory) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        assertThatThrownBy(() -> controller.invokeOperationSubmodelRepo(aasId, submodelId, "operations.doThing", body, true))
                .isInstanceOf(MethodNotImplementedException.class);
    }

    @Test
    void invokeOperationSubmodelRepo_propagatesSubmodelNotFoundException() {
        var submodelRepository = mock(SubmodelRepository.class);
        when(submodelRepository.invokeOperation(eq("deployment-42"), eq("operations.doThing"), any(OperationVariable[].class)))
                .thenThrow(new SubmodelNotFoundException("aas-1", "deployment-42"));

        var factory = mock(SubmodelRepositoryFactory.class);
        when(factory.getSubmodelRepository("aas-1")).thenReturn(submodelRepository);

        var controller = new MultiSubmodelRepositoryHTTPApiController(factory) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        assertThatThrownBy(() -> controller.invokeOperationSubmodelRepo(aasId, submodelId, "operations.doThing", body, false))
                .isInstanceOf(SubmodelNotFoundException.class);
    }

    @Test
    void invokeOperationSubmodelRepo_propagatesMethodNotImplementedException_whenUnderlyingServiceNotImplemented() {
        var submodelRepository = mock(SubmodelRepository.class);
        when(submodelRepository.invokeOperation(eq("deployment-42"), eq("operations.doThing"), any(OperationVariable[].class)))
                .thenThrow(new MethodNotImplementedException());

        var factory = mock(SubmodelRepositoryFactory.class);
        when(factory.getSubmodelRepository("aas-1")).thenReturn(submodelRepository);

        var controller = new MultiSubmodelRepositoryHTTPApiController(factory) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        assertThatThrownBy(() -> controller.invokeOperationSubmodelRepo(aasId, submodelId, "operations.doThing", body, false))
                .isInstanceOf(MethodNotImplementedException.class);
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
