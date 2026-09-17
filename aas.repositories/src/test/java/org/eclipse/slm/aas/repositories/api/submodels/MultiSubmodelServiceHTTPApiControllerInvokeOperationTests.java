package org.eclipse.slm.aas.repositories.api.submodels;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationRequest;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelservice.SubmodelService;
import org.eclipse.slm.aas.repositories.exceptions.MethodNotImplementedException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiSubmodelServiceHTTPApiControllerInvokeOperationTests {

    @Test
    void invokeOperation_returnsOperationResult_withOutputArguments() {
        var service = mock(SubmodelService.class);
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(service.invokeOperation(eq("operations.doThing"), any(OperationVariable[].class))).thenReturn(expectedOutput);

        var controller = new MultiSubmodelServiceHTTPApiController(service) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        var response = controller.invokeOperation("aas-1", "operations.doThing", body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getOutputArguments()).containsExactly(expectedOutput);
    }

    @Test
    void invokeOperation_propagatesMethodNotImplementedException_whenServiceDoesNotOverrideInvoke() {
        var service = mock(SubmodelService.class);
        when(service.invokeOperation(eq("operations.doThing"), any(OperationVariable[].class)))
                .thenThrow(new MethodNotImplementedException());

        var controller = new MultiSubmodelServiceHTTPApiController(service) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        assertThatThrownBy(() -> controller.invokeOperation("aas-1", "operations.doThing", body))
                .isInstanceOf(MethodNotImplementedException.class);
    }

    @Test
    void invokeOperation_propagatesElementDoesNotExistException_whenIdShortPathUnknown() {
        var service = mock(SubmodelService.class);
        when(service.invokeOperation(eq("operations.unknown"), any(OperationVariable[].class)))
                .thenThrow(new ElementDoesNotExistException("operations.unknown"));

        var controller = new MultiSubmodelServiceHTTPApiController(service) {};
        var body = new DefaultOperationRequest.Builder().inputArguments(List.of()).build();

        assertThatThrownBy(() -> controller.invokeOperation("aas-1", "operations.unknown", body))
                .isInstanceOf(ElementDoesNotExistException.class);
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
