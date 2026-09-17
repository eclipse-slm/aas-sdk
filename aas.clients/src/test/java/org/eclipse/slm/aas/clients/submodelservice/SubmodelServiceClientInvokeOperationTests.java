package org.eclipse.slm.aas.clients.submodelservice;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelservice.client.ConnectedSubmodelService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmodelServiceClientInvokeOperationTests {

    @Test
    void invokeOperation_returnsOutputArguments_fromConnectedSubmodelService() {
        var connectedSubmodelService = mock(ConnectedSubmodelService.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(connectedSubmodelService.invokeOperation("operations.doThing", input)).thenReturn(expectedOutput);

        var client = new SubmodelServiceClient("http://host/submodel", connectedSubmodelService);

        var output = client.invokeOperation("operations.doThing", input);

        assertThat(output).isSameAs(expectedOutput);
    }

    @Test
    void invokeOperation_propagatesElementDoesNotExistException_whenIdShortPathUnknown() {
        var connectedSubmodelService = mock(ConnectedSubmodelService.class);
        var input = new OperationVariable[0];
        when(connectedSubmodelService.invokeOperation("operations.unknown", input))
                .thenThrow(new ElementDoesNotExistException("operations.unknown"));

        var client = new SubmodelServiceClient("http://host/submodel", connectedSubmodelService);

        assertThatThrownBy(() -> client.invokeOperation("operations.unknown", input))
                .isInstanceOf(ElementDoesNotExistException.class);
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
