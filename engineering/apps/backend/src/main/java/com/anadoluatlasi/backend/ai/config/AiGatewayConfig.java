package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiVisualAssetAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiVisualAssetAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiPlatformPackageAgentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FP-001 + FP-002 + FP-003 + FP-004 + FP-006: AI gateway bean konfigürasyonu.
 *
 * <p>Provider seçimi {@code ai.provider} property ile yapılır:</p>
 * <ul>
 *   <li>{@code mock} (default) — Mock gateway'ler</li>
 *   <li>{@code openai} — OpenAI gateway'ler</li>
 * </ul>
 *
 * <p><strong>FP-001 Content Gateway:</strong> Provider seçimi {@code ai.provider} ile yapılır.
 * {@code mock} veya ayarlanmamışsa MockAiContentAgentGateway, {@code openai} ise
 * OpenAiContentAgentGateway aktifleşir.</p>
 *
 * <p><strong>FP-002 Scenario Gateway:</strong> {@code ai.provider=openai} olduğunda
 * {@link OpenAiScenarioAgentGateway} aktifleşir. Aksi halde
 * {@link MockAiScenarioAgentGateway} fallback olarak devreye girer.</p>
 *
 * <p><strong>FP-003 Storyboard Gateway:</strong> {@code ai.provider=openai} olduğunda
 * {@link OpenAiStoryboardAgentGateway} aktifleşir. Aksi halde
 * {@link MockAiStoryboardAgentGateway} fallback olarak devreye girer.</p>
 *
 * <p><strong>FP-006 Visual Asset Gateway:</strong> Mock fallback via
 * {@code @ConditionalOnMissingBean}. OpenAI provider Task 09'da eklenecektir.</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiGatewayConfig {

    // ── FP-001: Content Agent Gateway ───────────────────────

    /**
     * Mock content gateway — varsayılan provider.
     * {@code ai.provider} ayarlanmamışsa veya {@code mock} ise aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
    public AiContentAgentGateway mockAiContentAgentGateway() {
        log.info("AI Provider: MOCK — gerçek AI çağrısı yapılmaz.");
        return new MockAiContentAgentGateway();
    }

    /**
     * OpenAI content gateway — gerçek AI provider.
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

    // ── FP-002: Scenario Agent Gateway ──────────────────────

    /**
     * OpenAI scenario gateway — gerçek AI provider.
     * {@code ai.provider=openai} ayarlandığında aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiScenarioAgentGateway openAiScenarioAgentGateway(AiProviderProperties properties) {
        log.info("AI Scenario Provider: OPENAI — model={}",
                properties.getOpenai().getModel());
        return new OpenAiScenarioAgentGateway(properties.getOpenai());
    }

    /**
     * Mock scenario gateway — fallback provider.
     *
     * <p>{@code @ConditionalOnMissingBean} ile kayıtlıdır. {@code ai.provider=openai}
     * olduğunda {@code OpenAiScenarioAgentGateway} bean'i mevcut olacağından,
     * bu fallback devre dışı kalır. {@code ai.provider=mock} veya herhangi bir
     * gerçek scenario gateway yoksa aktifleşir.</p>
     */
    @Bean
    @ConditionalOnMissingBean(AiScenarioAgentGateway.class)
    public AiScenarioAgentGateway mockAiScenarioAgentGateway() {
        log.info("AI Scenario Provider: MOCK (fallback) — deterministic 6-beat senaryo üretir.");
        return new MockAiScenarioAgentGateway();
    }

    // ── FP-003: Storyboard Agent Gateway ────────────────────

    /**
     * OpenAI storyboard gateway — gerçek AI provider.
     * {@code ai.provider=openai} ayarlandığında aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiStoryboardAgentGateway openAiStoryboardAgentGateway(AiProviderProperties properties) {
        log.info("AI Storyboard Provider: OPENAI — model={}",
                properties.getOpenai().getModel());
        return new OpenAiStoryboardAgentGateway(properties.getOpenai());
    }

    /**
     * Mock storyboard gateway — fallback provider.
     *
     * <p>{@code @ConditionalOnMissingBean} ile kayıtlıdır. {@code ai.provider=openai}
     * olduğunda {@code OpenAiStoryboardAgentGateway} bean'i mevcut olacağından,
     * bu fallback devre dışı kalır.</p>
     */
    @Bean
    @ConditionalOnMissingBean(AiStoryboardAgentGateway.class)
    public AiStoryboardAgentGateway mockAiStoryboardAgentGateway() {
        log.info("AI Storyboard Provider: MOCK (fallback) — deterministic scene üretir.");
        return new MockAiStoryboardAgentGateway();
    }

    // ── FP-004: Platform Package Agent Gateway ──────────────

    /**
     * OpenAI platform package gateway — gerçek AI provider.
     * {@code ai.provider=openai} ayarlandığında aktifleşir.
     */
    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public AiPlatformPackageAgentGateway openAiPlatformPackageAgentGateway(AiProviderProperties properties) {
        log.info("AI Platform Package Provider: OPENAI — model={}",
                properties.getOpenai().getModel());
        return new OpenAiPlatformPackageAgentGateway(properties.getOpenai());
    }

    /**
     * Mock platform package gateway — fallback provider.
     *
     * <p>{@code @ConditionalOnMissingBean} ile kayıtlıdır. {@code ai.provider=openai}
     * olduğunda {@code OpenAiPlatformPackageAgentGateway} bean'i mevcut olacağından,
     * bu fallback devre dışı kalır.</p>
     */
    @Bean
    @ConditionalOnMissingBean(AiPlatformPackageAgentGateway.class)
    public AiPlatformPackageAgentGateway mockAiPlatformPackageAgentGateway() {
        log.info("AI Platform Package Provider: MOCK (fallback) — deterministic 4 platform paketi üretir.");
        return new MockAiPlatformPackageAgentGateway();
    }

    // ── FP-006: Visual Asset Agent Gateway ──────────────────

    /**
     * Mock visual asset gateway — fallback provider.
     *
     * <p>{@code @ConditionalOnMissingBean} ile kayıtlıdır.
     * OpenAI provider Task 09'da eklenecektir.</p>
     *
     * <p>Bu gateway yalnızca metin prompt üretir — image API çağırmaz.</p>
     */
    @Bean
    @ConditionalOnMissingBean(AiVisualAssetAgentGateway.class)
    public AiVisualAssetAgentGateway mockAiVisualAssetAgentGateway() {
        log.info("AI Visual Asset Provider: MOCK (fallback) — deterministic görsel plan prompt üretir.");
        return new MockAiVisualAssetAgentGateway();
    }
}
