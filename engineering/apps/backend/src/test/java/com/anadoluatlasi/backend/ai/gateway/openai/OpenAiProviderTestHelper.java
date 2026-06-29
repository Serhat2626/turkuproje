package com.anadoluatlasi.backend.ai.gateway.openai;

import com.anadoluatlasi.backend.ai.config.AiProviderProperties;
import com.anadoluatlasi.backend.ai.entity.AiErrorType;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * FP-001 Task 25: OpenAI gateway test yardımcısı.
 *
 * <p>OpenAiClient package-private olduğu için, bu helper sınıfı
 * diğer paketlerdeki testlerin mocked client ile gateway oluşturmasını sağlar.</p>
 *
 * <p>Dış ağa çıkış YOKTUR — tüm HTTP davranışı mock'lanır.</p>
 */
public class OpenAiProviderTestHelper {

    /**
     * Mocked OpenAiClient ile başarılı yanıt dönen gateway oluşturur.
     *
     * @param validJsonContent OpenAI model çıktısı olarak kullanılacak JSON string
     */
    public static OpenAiContentAgentGateway createGatewayWithMockedSuccess(String validJsonContent) {
        AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
        config.setApiKey("sk-test-integration-key");

        OpenAiClient mockClient = mock(OpenAiClient.class);
        when(mockClient.call(any(), any(), any()))
                .thenReturn(OpenAiCallResult.success(validJsonContent, 1000L, "input:300,output:200"));

        return new OpenAiContentAgentGateway(config, mockClient);
    }

    /**
     * Mocked OpenAiClient ile hata dönen gateway oluşturur.
     */
    public static OpenAiContentAgentGateway createGatewayWithMockedFailure(
            AiErrorType errorType, String message, boolean retryable) {
        AiProviderProperties.OpenAi config = new AiProviderProperties.OpenAi();
        config.setApiKey("sk-test-integration-key");

        OpenAiClient mockClient = mock(OpenAiClient.class);
        when(mockClient.call(any(), any(), any()))
                .thenReturn(OpenAiCallResult.failure(errorType, message, retryable, 500L));

        return new OpenAiContentAgentGateway(config, mockClient);
    }
}
