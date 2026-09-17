package org.eclipse.slm.aas.clients.submodelrepository;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmodelRepositoryClientInvokeOperationTests {

    @Test
    void invokeOperation_returnsOutputArguments_fromConnectedSubmodelRepository() {
        var connectedSubmodelRepository = mock(ConnectedSubmodelRepository.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(connectedSubmodelRepository.invokeOperation("deployment-42", "operations.doThing", input)).thenReturn(expectedOutput);

        var client = new SubmodelRepositoryClient("http://host/submodels", connectedSubmodelRepository);

        var output = client.invokeOperation("deployment-42", "operations.doThing", input);

        assertThat(output).isSameAs(expectedOutput);
    }

    @Test
    void invokeOperation_propagatesElementDoesNotExistException_whenIdShortPathUnknown() {
        var connectedSubmodelRepository = mock(ConnectedSubmodelRepository.class);
        var input = new OperationVariable[0];
        when(connectedSubmodelRepository.invokeOperation("deployment-42", "operations.unknown", input))
                .thenThrow(new ElementDoesNotExistException("operations.unknown"));

        var client = new SubmodelRepositoryClient("http://host/submodels", connectedSubmodelRepository);

        assertThatThrownBy(() -> client.invokeOperation("deployment-42", "operations.unknown", input))
                .isInstanceOf(ElementDoesNotExistException.class);
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
