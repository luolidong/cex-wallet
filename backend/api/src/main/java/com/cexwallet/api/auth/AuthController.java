package com.cexwallet.api.auth;

import com.cexwallet.api.auth.AuthDtos.AdminUserView;
import com.cexwallet.api.auth.AuthDtos.LoginRequest;
import com.cexwallet.api.auth.AuthDtos.LoginResponse;
import com.cexwallet.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }

    @GetMapping("/admin/profile")
    public ApiResponse<AdminUserView> profile(@AuthenticationPrincipal AdminUser adminUser) {
        return ApiResponse.ok(authService.currentUser(adminUser));
    }
}
