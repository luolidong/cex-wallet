package com.cexwallet.api.user;

import com.cexwallet.api.common.BusinessException;
import com.cexwallet.api.common.PageResponse;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String username, String email, String phone) {
        try {
            return userRepository.create(username, email, phone);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("CONFLICT", "username or email already exists", HttpStatus.CONFLICT);
        }
    }

    public PageResponse<User> findAll(String keyword, String status, Integer kycLevel, int page, int pageSize) {
        String normalizedStatus = normalizeOptionalStatus(status);
        Integer normalizedKycLevel = kycLevel == null ? null : normalizeKycLevel(kycLevel);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (normalizedPage - 1) * normalizedPageSize;
        List<User> items = userRepository.findAll(keyword, normalizedStatus, normalizedKycLevel, normalizedPageSize, offset);
        long total = userRepository.countAll(keyword, normalizedStatus, normalizedKycLevel);
        return new PageResponse<>(items, normalizedPage, normalizedPageSize, total);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "user not found", HttpStatus.NOT_FOUND));
    }

    public User updateStatus(Long id, String status) {
        String normalizedStatus = normalizeStatus(status);
        findById(id);
        if (!userRepository.updateStatus(id, normalizedStatus)) {
            throw new BusinessException("NOT_FOUND", "user not found", HttpStatus.NOT_FOUND);
        }
        return findById(id);
    }

    public User requireActive(Long id) {
        User user = findById(id);
        if (!"ACTIVE".equals(user.status())) {
            throw new BusinessException("USER_NOT_ACTIVE", "user is not active", HttpStatus.BAD_REQUEST);
        }
        return user;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException("INVALID_STATUS", "status is required", HttpStatus.BAD_REQUEST);
        }
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status)) {
            throw new BusinessException("INVALID_STATUS", "status must be ACTIVE or FROZEN", HttpStatus.BAD_REQUEST);
        }
        return status;
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return normalizeStatus(status);
    }

    public User updateKycLevel(Long id, Integer kycLevel) {
        int normalizedKycLevel = normalizeKycLevel(kycLevel);
        findById(id);
        if (!userRepository.updateKycLevel(id, normalizedKycLevel)) {
            throw new BusinessException("NOT_FOUND", "user not found", HttpStatus.NOT_FOUND);
        }
        return findById(id);
    }

    private int normalizeKycLevel(Integer kycLevel) {
        if (kycLevel == null) {
            throw new BusinessException("INVALID_KYC_LEVEL", "kyc level is required", HttpStatus.BAD_REQUEST);
        }
        if (kycLevel < 0 || kycLevel > 3) {
            throw new BusinessException("INVALID_KYC_LEVEL", "kyc level must be between 0 and 3", HttpStatus.BAD_REQUEST);
        }
        return kycLevel;
    }
}
