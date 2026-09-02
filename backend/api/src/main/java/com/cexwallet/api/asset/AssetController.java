package com.cexwallet.api.asset;

import com.cexwallet.api.asset.AssetDtos.ChainView;
import com.cexwallet.api.asset.AssetDtos.TokenView;
import com.cexwallet.api.asset.AssetDtos.UpdateChainRequest;
import com.cexwallet.api.asset.AssetDtos.UpdateTokenRequest;
import com.cexwallet.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping("/chains")
    public ApiResponse<List<ChainView>> chains() {
        return ApiResponse.ok(assetService.findChains());
    }

    @PutMapping("/chains/{id}")
    public ApiResponse<List<ChainView>> updateChain(@PathVariable Long id, @Valid @RequestBody UpdateChainRequest request) {
        return ApiResponse.ok(assetService.updateChain(id, request));
    }

    @GetMapping("/tokens")
    public ApiResponse<List<TokenView>> tokens() {
        return ApiResponse.ok(assetService.findTokens());
    }

    @PutMapping("/tokens/{id}")
    public ApiResponse<List<TokenView>> updateToken(@PathVariable Long id, @Valid @RequestBody UpdateTokenRequest request) {
        return ApiResponse.ok(assetService.updateToken(id, request));
    }
}
