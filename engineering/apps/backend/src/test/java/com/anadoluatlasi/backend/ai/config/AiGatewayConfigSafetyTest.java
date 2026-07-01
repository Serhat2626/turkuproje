package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiScenarioAgentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-002: AiGatewayConfig bean kayıt güvenliği testleri.
 *
 * <p>Bu testler, {@code ai.provider=openai} olduğunda bile
 * {@link AiScenarioAgentGateway} bean'inin context'te eksik
 * kalmamasını garanti eder.</p>
 */
class AiGatewayConfigSafetyTest {

    private final AiGatewayConfig config = new AiGatewayConfig();

    // ── Scenario Gateway Fallback Safety ─────────────────────

    @Nested
    @DisplayName("Scenario gateway fallback safety")
    class ScenarioGatewayFallback {

        @Test
        @DisplayName("Mock scenario gateway, @ConditionalOnMissingBean ile kayıtlıdır — ai.provider=openai güvenli")
        void mockScenarioGatewayShouldUseConditionalOnMissingBean() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("mockAiScenarioAgentGateway");

            // @ConditionalOnMissingBean olmalı
            ConditionalOnMissingBean annotation = method.getAnnotation(ConditionalOnMissingBean.class);
            assertThat(annotation)
                    .as("mockAiScenarioAgentGateway @ConditionalOnMissingBean ile annotate edilmeli")
                    .isNotNull();
            assertThat(annotation.value()).containsExactly(AiScenarioAgentGateway.class);

            // @ConditionalOnProperty olmamalı
            ConditionalOnProperty propertyAnnotation = method.getAnnotation(ConditionalOnProperty.class);
            assertThat(propertyAnnotation)
                    .as("mockAiScenarioAgentGateway @ConditionalOnProperty kullanmamalı — unsafe ai.provider binding")
                    .isNull();
        }

        @Test
        @DisplayName("Mock scenario gateway doğrudan çağrılabilir — geçerli bean üretir")
        void mockScenarioGatewayBeanShouldBeValid() {
            AiScenarioAgentGateway gateway = config.mockAiScenarioAgentGateway();

            assertThat(gateway).isNotNull();
            assertThat(gateway).isInstanceOf(MockAiScenarioAgentGateway.class);
        }

        @Test
        @DisplayName("ai.provider=openai senaryosu: scenario gateway fallback olarak mock döner")
        void openAiProviderShouldStillHaveScenarioGateway() {
            // ai.provider=openai olduğunda, @ConditionalOnMissingBean sayesinde
            // başka bir AiScenarioAgentGateway bean yoksa mock fallback devreye girer.
            // Bu test, mock bean'in her zaman oluşturulabilir olduğunu doğrular.
            AiScenarioAgentGateway fallback = config.mockAiScenarioAgentGateway();

            assertThat(fallback).isNotNull();
            assertThat(fallback).isInstanceOf(MockAiScenarioAgentGateway.class);
        }
    }

    // ── FP-001 Content Gateway Isolation ─────────────────────

    @Nested
    @DisplayName("FP-001 content gateway isolation")
    class ContentGatewayIsolation {

        @Test
        @DisplayName("Mock content gateway hâlâ @ConditionalOnProperty(mock) kullanır — FP-001 davranışı değişmedi")
        void mockContentGatewayShouldUseConditionalOnProperty() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("mockAiContentAgentGateway");

            ConditionalOnProperty annotation = method.getAnnotation(ConditionalOnProperty.class);
            assertThat(annotation)
                    .as("FP-001 mock content gateway @ConditionalOnProperty ile kalmalı")
                    .isNotNull();
            assertThat(annotation.name()).containsExactly("ai.provider");
            assertThat(annotation.havingValue()).isEqualTo("mock");
            assertThat(annotation.matchIfMissing()).isTrue();
        }

        @Test
        @DisplayName("Mock content gateway doğrudan çağrılabilir — geçerli bean üretir")
        void mockContentGatewayBeanShouldBeValid() {
            AiContentAgentGateway gateway = config.mockAiContentAgentGateway();

            assertThat(gateway).isNotNull();
            assertThat(gateway).isInstanceOf(MockAiContentAgentGateway.class);
        }

        @Test
        @DisplayName("OpenAI content gateway hâlâ @ConditionalOnProperty(openai) kullanır — FP-001 davranışı değişmedi")
        void openAiContentGatewayShouldUseConditionalOnProperty() throws Exception {
            Method method = AiGatewayConfig.class.getDeclaredMethod("openAiContentAgentGateway", AiProviderProperties.class);

            ConditionalOnProperty annotation = method.getAnnotation(ConditionalOnProperty.class);
            assertThat(annotation)
                    .as("FP-001 OpenAI content gateway @ConditionalOnProperty ile kalmalı")
                    .isNotNull();
            assertThat(annotation.name()).containsExactly("ai.provider");
            assertThat(annotation.havingValue()).isEqualTo("openai");
        }
    }
}
