package com.anadoluatlasi.backend.health.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Sistem sağlık kontrolü endpoint'i.
 * Uygulamanın ayakta olup olmadığını kontrol etmek için kullanılır.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Sistem sağlık kontrolü")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Sistem sağlık durumu", description = "Uygulamanın çalışır durumda olup olmadığını kontrol eder")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "anadolu-atlasi-backend"
        ));
    }
}
