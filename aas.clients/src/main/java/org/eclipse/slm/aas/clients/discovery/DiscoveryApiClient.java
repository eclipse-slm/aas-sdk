package org.eclipse.slm.aas.clients.discovery;

import feign.RequestLine;
import feign.Param;
import org.eclipse.slm.aas.model.discovery.respones.GetAasIdsByAssetLinkResults;
import org.eclipse.slm.aas.model.shared.PagedResult;

public interface DiscoveryApiClient {

    @RequestLine("GET /lookup/shells?assetId={assetId}")
    String[] getAllAssetAdministrationShellIdsByAssetId(@Param("assetId") String assetId);

    /**
     * Required for BaSyx Discovery Service
     **/
    @RequestLine("GET /lookup/shells?assetIds={assetIds}")
    GetAasIdsByAssetLinkResults getAllAssetAdministrationShellIdsByAssetLink(@Param("assetIds") String assetIds);
}
