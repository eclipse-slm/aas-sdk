package org.eclipse.slm.aas.repositories.submodels;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.slm.aas.repositories.exceptions.MethodNotImplementedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractSubmodelServiceInvokeOperationTests {

    @Test
    void invokeOperation_throwsMethodNotImplementedException_byDefault() {
        var service = new AbstractSubmodelService() {
            @Override
            public Submodel getSubmodel() {
                return null;
            }
        };

        assertThatThrownBy(() -> service.invokeOperation("operations.doThing", new OperationVariable[0]))
                .isInstanceOf(MethodNotImplementedException.class);
    }
}
