package org.eclipse.slm.aas.clients.submodelservice;

import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.slm.aas.clients.submodelrepository.SubmodelRepositoryClientFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Invokes an AAS operation given only a {@link SubmodelDescriptor} and a JWT, without the
 * caller needing to know whether the descriptor's endpoint is a standalone submodel service
 * ({@code .../submodel}) or a submodel repository entry ({@code .../submodels/{id}}).
 */
public class SubmodelOperationInvoker {

    private SubmodelOperationInvoker() {
    }

    public static OperationVariable[] invokeOperation(SubmodelDescriptor submodelDescriptor, JwtAuthenticationToken jwtAuthenticationToken, String idShortPath, OperationVariable[] input) throws ElementDoesNotExistException {
        var submodelEndpoint = submodelDescriptor.getEndpoints().get(0).getProtocolInformation().getHref();

        if (isSubmodelServiceEndpoint(submodelEndpoint)) {
            var submodelServiceClient = SubmodelServiceClient.FromSubmodelDescriptor(submodelDescriptor, jwtAuthenticationToken);
            return submodelServiceClient.invokeOperation(idShortPath, input);
        }

        var submodelRepositoryClient = SubmodelRepositoryClientFactory.FromSubmodelDescriptor(submodelDescriptor, jwtAuthenticationToken);
        return submodelRepositoryClient.invokeOperation(submodelDescriptor.getId(), idShortPath, input);
    }

    static boolean isSubmodelServiceEndpoint(String submodelEndpoint) {
        return submodelEndpoint.endsWith("/submodel");
    }
}
