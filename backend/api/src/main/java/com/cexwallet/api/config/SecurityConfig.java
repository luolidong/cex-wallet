package com.cexwallet.api.config;

import com.cexwallet.api.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/actuator/health", "/api/auth/login", "/api/internal/**").permitAll()
                        .requestMatchers("/api/admin-management/**").hasAuthority("admin:manage")
                        .requestMatchers("/api/audit-logs/**").hasAuthority("audit:read")
                        .requestMatchers("/api/reconciliation/**").hasAuthority("reconciliation:read")
                        .requestMatchers("/api/scanner/**").hasAuthority("scanner:read")
                        .requestMatchers("/api/system/**").hasAuthority("system:read")
                        .requestMatchers("/api/assets/**").hasAuthority("asset:manage")
                        .requestMatchers("/api/risk/**").hasAuthority("risk:manage")
                        .requestMatchers(HttpMethod.GET, "/api/ledger/**").hasAuthority("ledger:read")
                        .requestMatchers(HttpMethod.GET, "/api/withdrawal-records/**").hasAuthority("withdrawal:review")
                        .requestMatchers("/api/withdrawals/**").hasAuthority("withdrawal:review")
                        .requestMatchers(HttpMethod.GET, "/api/deposits/**").hasAuthority("wallet:read")
                        .requestMatchers(HttpMethod.GET, "/api/wallets/**").hasAuthority("wallet:read")
                        .requestMatchers("/api/wallets/**").hasAuthority("wallet:manage")
                        .requestMatchers("/api/users/*/withdrawals/**").hasAuthority("withdrawal:review")
                        .requestMatchers("/api/users/**").hasAuthority("user:read")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> writeUnauthorized(response))
                        .accessDeniedHandler((request, response, accessDeniedException) -> writeForbidden(response))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("UNAUTHORIZED", "unauthorized", null));
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail("FORBIDDEN", "forbidden", null));
    }
}
