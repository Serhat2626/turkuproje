package com.anadoluatlasi.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Anadolu Atlası Backend uygulaması ana sınıfı.
 * AI destekli çok kanallı içerik üretim platformu.
 */
@SpringBootApplication
@EnableJpaAuditing
public class AnadoluAtlasiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnadoluAtlasiBackendApplication.class, args);
    }
}
