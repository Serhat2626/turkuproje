package com.anadoluatlasi.backend.auth.dto;

import com.anadoluatlasi.backend.auth.entity.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication yanıt DTO. Token + kullanıcı bilgisi içerir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private String username;
    private String displayName;
    private AdminRole role;

    public static AuthResponse of(String token, long expiresInMs, String username,
                                   String displayName, AdminRole role) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresInMs(expiresInMs)
                .username(username)
                .displayName(displayName)
                .role(role)
                .build();
    }
}
