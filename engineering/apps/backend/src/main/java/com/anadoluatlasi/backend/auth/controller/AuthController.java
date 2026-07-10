package com.anadoluatlasi.backend.auth.controller;

import com.anadoluatlasi.backend.auth.dto.AuthResponse;
import com.anadoluatlasi.backend.auth.dto.LoginRequest;
import com.anadoluatlasi.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication controller — login ve kullanıcı bilgisi endpoint'leri.
 *
 * <p>Tüm endpoint'ler /api/auth/ altında. Login herkese açık,
 * /me endpoint'i authenticated kullanıcı gerektirir.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Kimlik doğrulama işlemleri")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Giriş yap", description = "Kullanıcı adı ve şifre ile JWT token al")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Mevcut kullanıcı bilgisi", description = "JWT token ile giriş yapmış kullanıcının bilgilerini döndürür")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

        Map<String, Object> userInfo = Map.of(
                "username", authentication.getName(),
                "role", role,
                "authenticated", true
        );

        return ResponseEntity.ok(userInfo);
    }
}
