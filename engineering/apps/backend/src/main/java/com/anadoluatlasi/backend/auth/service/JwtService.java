package com.anadoluatlasi.backend.auth.service;

import com.anadoluatlasi.backend.auth.entity.AdminRole;
import com.anadoluatlasi.backend.auth.entity.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token üretimi ve doğrulaması.
 *
 * <p>Token içeriği: subject=username, role claim, iat, exp.
 * Token'da şifre, secret veya hassas veri saklanmaz.</p>
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Kullanıcı için JWT token üret.
     */
    public String generateToken(AdminUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Token'dan username çıkar.
     */
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Token'dan rol çıkar.
     */
    public AdminRole extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        return AdminRole.valueOf(role);
    }

    /**
     * Token geçerliliğini doğrula.
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Debug seviyesi — expired token normal akış (frontend 401'de login'e yönlendirir)
            log.debug("JWT token süresi dolmuş: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Geçersiz JWT token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Token expiration süresini döndür (ms).
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
