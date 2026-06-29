package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiContentAgentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FP-001: AI gateway bean konfigürasyonu.
 * Varsayılan olarak MockAiContentAgentGateway kullanılır.
 * Gerçek provider eklendiğinde bu konfigürasyon profil veya conditional ile yönetilecek.
 */
@Configuration
public class AiGatewayConfig {

    @Bean
    public AiContentAgentGateway aiContentAgentGateway() {
        return new MockAiContentAgentGateway();
    }
}
