package org.eclipse.slm.aas.repositories.submodels;

public interface SubmodelRepositoryFactory {

    SubmodelRepository getSubmodelRepository(String aasId);

}
