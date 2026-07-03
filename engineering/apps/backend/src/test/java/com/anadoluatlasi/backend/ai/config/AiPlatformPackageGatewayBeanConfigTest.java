package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiContentAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiPlatformPackageAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiPlatformPackageAgentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-004 Task 09: Platform package gateway bean konfigürasyonu testleri.
 *
 * <p>Doğrulama noktaları:</p>
 * <ul>
 *   <li>ai.provider=openai → OpenAiPlatformPackageAgentGateway aktif</li>
 *   <li>ai.provider=mock → MockAiPlatformPackageAgentGateway fallback aktif</li>
 *   <li>Mock fallback @ConditionalOnMissingBean ile kayıtlı — OpenAI bean varsa override etmez</li>
 *   <li>Mevcut content/scenario/storyboard gateway bean'leri etkilenmez</li>
 * </ul>
 */
@DisplayName("FP-004 Task 09: AiPlatformPackageGatewayBeanConfig")
class AiPlatformPackageGatewayBeanConfigTest {

    // ── OpenAI Provider ─────────────────────────────────────

    @Nested
    @DisplayName("ai.provider=openai")
    @SpringBootTest(classes = AiGatewayConfig.class)
    @TestPropertySource(properties = {
            "ai.provider=openai",
            "ai.openai.api-key=test-key-bean-config",
            "ai.openai.model=gpt-4o-mini",
            "ai.openai.base-url=https://api.openai.com/v1",
            "ai.openai.timeout-seconds=60",
            "ai.openai.max-output-tokens=4000",
            "ai.openai.temperature=0.4"
    })
    class OpenAiProvider {

        @Autowired
        AiPlatformPackageAgentGateway platformPackageGateway;

        @Autowired
        AiContentAgentGateway contentGateway;

        @Autowired
        AiScenarioAgentGateway scenarioGateway;

        @Autowired
        AiStoryboardAgentGateway storyboardGateway;

        @Test
        @DisplayName("OpenAI platform package gateway bean aktif")
        void openAiPlatformPackageGatewayIsActive() {
            assertThat(platformPackageGateway).isNotNull();
            assertThat(platformPackageGateway).isInstanceOf(OpenAiPlatformPackageAgentGateway.class);
        }

        @Test
        @DisplayName("Content gateway etkilenmez")
        void contentGatewayNotAffected() {
            assertThat(contentGateway).isNotNull();
        }

        @Test
        @DisplayName("Scenario gateway etkilenmez")
        void scenarioGatewayNotAffected() {
            assertThat(scenarioGateway).isNotNull();
        }

        @Test
        @DisplayName("Storyboard gateway etkilenmez")
        void storyboardGatewayNotAffected() {
            assertThat(storyboardGateway).isNotNull();
        }
    }

    // ── Mock Provider ───────────────────────────────────────

    @Nested
    @DisplayName("ai.provider=mock")
    @SpringBootTest(classes = AiGatewayConfig.class)
    @TestPropertySource(properties = {
            "ai.provider=mock"
    })
    class MockProvider {

        @Autowired
        AiPlatformPackageAgentGateway platformPackageGateway;

        @Test
        @DisplayName("Mock platform package gateway fallback aktif")
        void mockPlatformPackageGatewayIsActive() {
            assertThat(platformPackageGateway).isNotNull();
            assertThat(platformPackageGateway).isInstanceOf(MockAiPlatformPackageAgentGateway.class);
        }
    }

    // ── Bean Safety ─────────────────────────────────────────

    @Nested
    @DisplayName("PlatformPackageGatewayFallback")
    class PlatformPackageGatewayFallback {

        @Test
        @DisplayName("mockAiPlatformPackageAgentGateway @ConditionalOnMissingBean ile kayıtlı")
        void mockFallbackHasConditionalOnMissingBean() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("mockAiPlatformPackageAgentGateway");
            ConditionalOnMissingBean annotation = method.getAnnotation(ConditionalOnMissingBean.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).containsExactly(AiPlatformPackageAgentGateway.class);
        }

        @Test
        @DisplayName("openAiPlatformPackageAgentGateway @ConditionalOnProperty ile kayıtlı")
        void openAiBeanHasConditionalOnProperty() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("openAiPlatformPackageAgentGateway",
                    AiProviderProperties.class);

            assertThat(method.getAnnotation(
                    org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                    .isNotNull();
        }

        @Test
        @DisplayName("Mock fallback bağımsız olarak oluşturulabilir")
        void mockFallbackCanBeCreatedIndependently() {
            MockAiPlatformPackageAgentGateway mock = new MockAiPlatformPackageAgentGateway();
            assertThat(mock).isInstanceOf(AiPlatformPackageAgentGateway.class);
        }

        @Test
        @DisplayName("Mock fallback OpenAI gateway'i override edemez (annotation kontrolü)")
        void mockFallbackDoesNotOverrideOpenAi() throws Exception {
            // @ConditionalOnMissingBean olduğu için OpenAI aktifken mock devre dışı kalır
            Method mockMethod = AiGatewayConfig.class.getMethod("mockAiPlatformPackageAgentGateway");
            ConditionalOnMissingBean annotation = mockMethod.getAnnotation(ConditionalOnMissingBean.class);
            assertThat(annotation).isNotNull();

            // OpenAI @ConditionalOnProperty olduğu için sadece openai'de aktif
            Method openAiMethod = AiGatewayConfig.class.getMethod("openAiPlatformPackageAgentGateway",
                    AiProviderProperties.class);
            assertThat(openAiMethod.getAnnotation(
                    org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                    .isNotNull();
        }
    }
}
