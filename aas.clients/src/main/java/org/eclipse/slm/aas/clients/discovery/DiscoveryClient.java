package org.eclipse.slm.aas.clients.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.slm.aas.clients.auth.AuthRequestInterceptor;
import org.eclipse.slm.aas.clients.base.FeignClientFactory;
import org.eclipse.slm.aas.model.discovery.AssetLink;
import org.eclipse.slm.aas.model.discovery.expcetions.DiscoveryClientRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DiscoveryClient {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DiscoveryApiClient discoveryApiClient;

    public DiscoveryClient(String aasDiscoveryUrl, AuthRequestInterceptor authRequestInterceptor) {
        this.discoveryApiClient = FeignClientFactory.createClient(DiscoveryApiClient.class, aasDiscoveryUrl, authRequestInterceptor);
    }

    public String[] getAllAssetAdministrationShellIdsByAssetId(String assetId) {
        var assetIds = this.discoveryApiClient.getAllAssetAdministrationShellIdsByAssetId(assetId);

        return assetIds;
    }

    public String[] getAllAssetAdministrationShellIdsByAssetLink(AssetLink assetLink) {
        try {
            var assetLinkJson = OBJECT_MAPPER.writeValueAsString(assetLink);
            var assetLinksEncoded = Base64.getEncoder().encodeToString(assetLinkJson.getBytes(StandardCharsets.UTF_8));

            var result = this.discoveryApiClient.getAllAssetAdministrationShellIdsByAssetLink(assetLinksEncoded);
            return result.getResult().toArray(new String[0]);
        } catch (Exception e) {
            throw new DiscoveryClientRuntimeException("Failed to lookup shell ids by asset link", e);
        }
    }
}
