package com.cexwallet.api.config;

import com.cexwallet.api.auth.AdminUser;
import com.cexwallet.api.auth.AdminUserRepository;
import com.cexwallet.api.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AdminUserRepository adminUserRepository) {
        this.jwtService = jwtService;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            Long adminUserId = jwtService.parseAdminUserId(authorization.substring(7));
            if (adminUserId != null) {
                adminUserRepository.findById(adminUserId)
                        .filter(user -> "ACTIVE".equals(user.status()))
                        .ifPresent(this::authenticate);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(AdminUser adminUser) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                adminUser,
                null,
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

