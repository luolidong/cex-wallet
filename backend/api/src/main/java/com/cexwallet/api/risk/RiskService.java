package com.cexwallet.api.risk;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.risk.RiskDtos.BlacklistAddressView;
import com.cexwallet.api.risk.RiskDtos.ChainOptionView;
import com.cexwallet.api.risk.RiskDtos.WithdrawalRuleView;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskService {
    private final RiskRepository riskRepository;

    public RiskService(RiskRepository riskRepository) {
        this.riskRepository = riskRepository;
    }

    public List<WithdrawalRuleView> findWithdrawalRules() {
        return riskRepository.findWithdrawalRules();
    }

    public List<ChainOptionView> findChains() {
        return riskRepository.findChains();
    }

    @Transactional
    public List<WithdrawalRuleView> updateWithdrawalRule(Long tokenId, BigDecimal maxWithdrawAmount, BigDecimal dailyWithdrawLimit) {
        if (maxWithdrawAmount != null && dailyWithdrawLimit != null && maxWithdrawAmount.compareTo(dailyWithdrawLimit) > 0) {
            throw new BusinessException("INVALID_WITHDRAW_LIMIT", "single withdrawal limit cannot exceed daily limit", HttpStatus.BAD_REQUEST);
        }
        if (!riskRepository.updateWithdrawalRule(tokenId, maxWithdrawAmount, dailyWithdrawLimit)) {
            throw new BusinessException("NOT_FOUND", "token not found", HttpStatus.NOT_FOUND);
        }
        return riskRepository.findWithdrawalRules();
    }

    public List<BlacklistAddressView> findBlacklistAddresses() {
        return riskRepository.findBlacklistAddresses();
    }

    @Transactional
    public BlacklistAddressView addBlacklistAddress(Long chainId, String address, String reason) {
        return riskRepository.upsertBlacklistAddress(chainId, address.trim(), reason);
    }

    @Transactional
    public List<BlacklistAddressView> disableBlacklistAddress(Long id) {
        if (!riskRepository.disableBlacklistAddress(id)) {
            throw new BusinessException("NOT_FOUND", "blacklist address not found", HttpStatus.NOT_FOUND);
        }
        return riskRepository.findBlacklistAddresses();
    }
}
