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
}

