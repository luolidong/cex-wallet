package com.cexwallet.api.asset;

import com.cexwallet.api.asset.AssetDtos.ChainView;
import com.cexwallet.api.asset.AssetDtos.TokenView;
import com.cexwallet.api.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {
    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<ChainView> findChains() {
        return assetRepository.findChains();
    }

    @Transactional
    public List<ChainView> updateChain(Long id, AssetDtos.UpdateChainRequest request) {
        if (!assetRepository.updateChain(id, request)) {
            throw new BusinessException("NOT_FOUND", "chain not found", HttpStatus.NOT_FOUND);
        }
        return assetRepository.findChains();
    }

    public List<TokenView> findTokens() {
        return assetRepository.findTokens();
    }

    @Transactional
    public List<TokenView> updateToken(Long id, AssetDtos.UpdateTokenRequest request) {
        if (!assetRepository.updateToken(id, request)) {
            throw new BusinessException("NOT_FOUND", "token not found", HttpStatus.NOT_FOUND);
        }
        return assetRepository.findTokens();
    }
}
