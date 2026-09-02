package com.cexwallet.api.auth;

import com.cexwallet.api.auth.AuthDtos.AdminUserView;
import com.cexwallet.api.auth.AuthDtos.LoginResponse;
import com.cexwallet.api.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(String username, String password) {
        AdminUser adminUser = adminUserRepository.findByUsername(username)
                .filter(user -> "ACTIVE".equals(user.status()))
                .orElseThrow(() -> new BusinessException("UNAUTHORIZED", "invalid username or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(password, adminUser.passwordHash())) {
            throw new BusinessException("UNAUTHORIZED", "invalid username or password", HttpStatus.UNAUTHORIZED);
        }

        adminUserRepository.updateLastLoginAt(adminUser.id());
        return new LoginResponse(jwtService.createToken(adminUser), jwtService.getExpiresSeconds(), toView(adminUser));
    }

    public AdminUserView currentUser(AdminUser adminUser) {
        return toView(adminUser);
    }

    private AdminUserView toView(AdminUser adminUser) {
        List<String> permissions = adminUserRepository.findPermissions(adminUser.id());
        return new AdminUserView(adminUser.id(), adminUser.username(), adminUser.displayName(), permissions);
    }
}

