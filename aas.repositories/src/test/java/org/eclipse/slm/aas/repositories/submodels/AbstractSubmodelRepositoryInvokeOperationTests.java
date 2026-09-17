package org.eclipse.slm.aas.repositories.submodels;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelservice.SubmodelService;
import org.eclipse.slm.aas.repositories.exceptions.SubmodelNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbstractSubmodelRepositoryInvokeOperationTests {

    @Test
    void invokeOperation_delegatesToSubmodelService_whenSubmodelServiceFactoryMatchesPrefix() {
        var submodelService = mock(SubmodelService.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(submodelService.invokeOperation("operations.doThing", input)).thenReturn(expectedOutput);

        var repository = new AbstractSubmodelRepository("aas-1") {};
        repository.addSubmodelServiceFactory("deployment", aasId -> submodelService);

        var output = repository.invokeOperation("deployment-42", "operations.doThing", input);

        assertThat(output).isSameAs(expectedOutput);
    }

    @Test
    void invokeOperation_delegatesToSubmodelRepository_whenSubmodelRepositoryFactoryMatchesPrefix() {
        var submodelRepository = mock(SubmodelRepository.class);
        var input = new OperationVariable[0];
        var expectedOutput = new OperationVariable[] { operationVariable("result", "42") };
        when(submodelRepository.invokeOperation("resources-42", "operations.doThing", input)).thenReturn(expectedOutput);

        var repository = new AbstractSubmodelRepository("aas-1") {};
        repository.setSubmodelRepositoryFactories(Map.of("resources", aasId -> submodelRepository));

        var output = repository.invokeOperation("resources-42", "operations.doThing", input);

        assertThat(output).isSameAs(expectedOutput);
    }

    @Test
    void invokeOperation_throwsSubmodelNotFoundException_whenNoFactoryMatchesPrefix() {
        var repository = new AbstractSubmodelRepository("aas-1") {};

        assertThatThrownBy(() -> repository.invokeOperation("unregistered-42", "operations.doThing", new OperationVariable[0]))
                .isInstanceOf(SubmodelNotFoundException.class);
    }

    @Test
    void invokeOperation_propagatesElementDoesNotExistException_whenIdShortPathUnknown() {
        var submodelService = mock(SubmodelService.class);
        when(submodelService.invokeOperation("operations.unknown", new OperationVariable[0]))
                .thenThrow(new ElementDoesNotExistException("operations.unknown"));

        var repository = new AbstractSubmodelRepository("aas-1") {};
        repository.addSubmodelServiceFactory("deployment", aasId -> submodelService);

        assertThatThrownBy(() -> repository.invokeOperation("deployment-42", "operations.unknown", new OperationVariable[0]))
                .isInstanceOf(ElementDoesNotExistException.class);
    }

    private OperationVariable operationVariable(String idShort, String value) {
        return new DefaultOperationVariable.Builder()
                .value(new DefaultProperty.Builder().idShort(idShort).value(value).build())
                .build();
    }
}
