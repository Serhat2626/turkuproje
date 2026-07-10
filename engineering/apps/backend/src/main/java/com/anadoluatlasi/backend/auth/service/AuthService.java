package com.anadoluatlasi.backend.auth.service;

import com.anadoluatlasi.backend.auth.dto.AuthResponse;
import com.anadoluatlasi.backend.auth.dto.LoginRequest;
import com.anadoluatlasi.backend.auth.entity.AdminUser;
import com.anadoluatlasi.backend.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Authentication iş mantığı servisi.
 *
 * <p>Login doğrulaması, JWT üretimi ve kullanıcı bilgisi döndürür.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminUserRepository adminUserRepository;

    /**
     * Kullanıcı girişi — kimlik doğrulama + JWT üretimi.
     */
    public AuthResponse login(LoginRequest request) {
        // Spring Security authentication manager ile doğrula
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Kullanıcıyı yükle
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));

        // Son giriş zamanını güncelle
        user.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(user);

        // JWT token üret
        String token = jwtService.generateToken(user);

        log.info("Başarılı giriş: username={}, role={}", user.getUsername(), user.getRole());

        return AuthResponse.of(
                token,
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole()
        );
    }
}
