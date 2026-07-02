package com.anadoluatlasi.backend.ai.config;

import com.anadoluatlasi.backend.ai.gateway.AiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.mock.MockAiScenarioAgentGateway;
import com.anadoluatlasi.backend.ai.gateway.openai.OpenAiScenarioAgentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FP-002 Task 09: Bean configuration tests for scenario gateway.
 */
class AiScenarioGatewayBeanConfigTest {

    @Nested
    @DisplayName("ai.provider=openai")
    @SpringBootTest(classes = AiGatewayConfig.class)
    @EnableConfigurationProperties(AiProviderProperties.class)
    @TestPropertySource(properties = {
            "ai.provider=openai",
            "ai.openai.api-key=test-key-not-real",
            "ai.openai.model=gpt-4o-mini"
    })
    class OpenAiProviderConfig {

        @Autowired
        private AiScenarioAgentGateway scenarioGateway;

        @Test
        @DisplayName("ai.provider=openai resolves OpenAiScenarioAgentGateway")
        void openaiResolvesRealGateway() {
            assertThat(scenarioGateway).isInstanceOf(OpenAiScenarioAgentGateway.class);
        }
    }

    @Nested
    @DisplayName("ai.provider=mock (fallback)")
    @SpringBootTest(classes = AiGatewayConfig.class)
    @EnableConfigurationProperties(AiProviderProperties.class)
    @TestPropertySource(properties = {
            "ai.provider=mock"
    })
    class MockProviderConfig {

        @Autowired
        private AiScenarioAgentGateway scenarioGateway;

        @Test
        @DisplayName("ai.provider=mock resolves MockAiScenarioAgentGateway")
        void mockResolvesFallbackGateway() {
            assertThat(scenarioGateway).isInstanceOf(MockAiScenarioAgentGateway.class);
        }
    }
}
