package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiStoryboardAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiStoryboardAgentGateway;
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
 * FP-003 Task 09: Storyboard gateway bean konfigürasyonu testleri.
 *
 * <p>Doğrulama noktaları:</p>
 * <ul>
 *   <li>ai.provider=openai → OpenAiStoryboardAgentGateway aktif</li>
 *   <li>ai.provider=mock → MockAiStoryboardAgentGateway fallback aktif</li>
 *   <li>Mock fallback @ConditionalOnMissingBean ile kayıtlı — OpenAI bean varsa override etmez</li>
 * </ul>
 */
@DisplayName("FP-003 Task 09: AiStoryboardGatewayBeanConfig")
class AiStoryboardGatewayBeanConfigTest {

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
            "ai.openai.max-output-tokens=2000",
            "ai.openai.temperature=0.4"
    })
    class OpenAiProvider {

        @Autowired
        AiStoryboardAgentGateway storyboardGateway;

        @Test
        @DisplayName("OpenAI storyboard gateway bean aktif")
        void openAiStoryboardGatewayIsActive() {
            assertThat(storyboardGateway).isNotNull();
            assertThat(storyboardGateway).isInstanceOf(OpenAiStoryboardAgentGateway.class);
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
        AiStoryboardAgentGateway storyboardGateway;

        @Test
        @DisplayName("Mock storyboard gateway fallback aktif")
        void mockStoryboardGatewayIsActive() {
            assertThat(storyboardGateway).isNotNull();
            assertThat(storyboardGateway).isInstanceOf(MockAiStoryboardAgentGateway.class);
        }
    }

    // ── Bean Safety ─────────────────────────────────────────

    @Nested
    @DisplayName("StoryboardGatewayFallback")
    class StoryboardGatewayFallback {

        @Test
        @DisplayName("mockAiStoryboardAgentGateway @ConditionalOnMissingBean ile kayıtlı")
        void mockFallbackHasConditionalOnMissingBean() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("mockAiStoryboardAgentGateway");
            ConditionalOnMissingBean annotation = method.getAnnotation(ConditionalOnMissingBean.class);

            assertThat(annotation).isNotNull();
            assertThat(annotation.value()).containsExactly(AiStoryboardAgentGateway.class);
        }

        @Test
        @DisplayName("openAiStoryboardAgentGateway @ConditionalOnProperty ile kayıtlı")
        void openAiBeanHasConditionalOnProperty() throws Exception {
            Method method = AiGatewayConfig.class.getMethod("openAiStoryboardAgentGateway",
                    AiProviderProperties.class);

            assertThat(method.getAnnotation(
                    org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                    .isNotNull();
        }

        @Test
        @DisplayName("Mock fallback bağımsız olarak oluşturulabilir")
        void mockFallbackCanBeCreatedIndependently() {
            MockAiStoryboardAgentGateway mock = new MockAiStoryboardAgentGateway();
            assertThat(mock).isInstanceOf(AiStoryboardAgentGateway.class);
        }
    }
}
