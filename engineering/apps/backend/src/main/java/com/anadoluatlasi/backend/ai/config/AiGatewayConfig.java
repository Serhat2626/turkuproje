package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiContentAgentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FP-001: AI gateway bean konfigürasyonu.
 *
 * <p>Provider seçimi {@code ai.provider} property ile yapılır:</p>
 * <ul>
 *   <li>{@code mock} (default) — MockAiContentAgentGateway</li>
 *   <li>{@code openai} — OpenAiContentAgentGateway</li>
 * </ul>
 *
 * <p>Aynı anda yalnızca bir gateway bean aktiftir.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiGatewayConfig {

    /**
     * Mock gateway — varsayılan provider.
     * {@code ai.provider} ayarlanmamışsa veya {@code mock} ise aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
    public AiContentAgentGateway mockAiContentAgentGateway() {
        log.info("AI Provider: MOCK — gerçek AI çağrısı yapılmaz.");
        return new MockAiContentAgentGateway();
    }

    /**
     * OpenAI gateway — gerçek AI provider.
     * {@code ai.provider=openai} ayarlandığında aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiContentAgentGateway openAiContentAgentGateway(AiProviderProperties properties) {
        log.info("AI Provider: OPENAI — model={}, baseUrl={}",
                properties.getOpenai().getModel(),
                properties.getOpenai().getBaseUrl());
        return new OpenAiContentAgentGateway(properties.getOpenai());
    }
}
