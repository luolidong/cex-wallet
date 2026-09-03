package com.cexwallet.api.user;

import com.cexwallet.api.common.BusinessException;
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

    public List<User> findAll(int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.min(Math.max(pageSize, 1), 100);
        return userRepository.findAll(normalizedPageSize, (normalizedPage - 1) * normalizedPageSize);
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
}
