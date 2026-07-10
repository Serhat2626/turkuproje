package com.anadoluatlasi.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BCrypt hash üretici — V17 migration'daki admin şifresi için.
 */
class BcryptHashGeneratorTest {

    @Test
    void generateAdminPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Anadolu.Atlasi.2026!";
        String hash = encoder.encode(password);

        System.out.println("==== BCRYPT HASH ====");
        System.out.println("Password: " + password);
        System.out.println("Hash: " + hash);
        System.out.println("====================");

        // Hash doğru üretildi mi?
        assertThat(encoder.matches(password, hash)).isTrue();
    }
}
