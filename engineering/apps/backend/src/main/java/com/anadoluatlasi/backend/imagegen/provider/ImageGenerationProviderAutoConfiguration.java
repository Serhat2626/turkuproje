package com.anadoluatlasi.backend.imagegen.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * FP-007: Provider gateway bean configuration.
 *
 * <p>Provider seçimi {@code imagegen.provider.mode} config property ile yapılır.</p>
 * <ul>
 *   <li>{@code mock} (default): MockImageGenerationProviderGateway — external call yok</li>
 *   <li>{@code openai}: OpenAiImageGenerationProviderGateway — real provider, explicit config gerekli</li>
 * </ul>
 *
 * <p>API key/secret bu class'ta tutulmaz, loglanmaz, DTO/entity'ye yazılmaz.
 * Config DTO/API response'a çıkmaz.</p>
 */
@Slf4j
@Configuration
public class ImageGenerationProviderAutoConfiguration {

    /**
     * Primary provider gateway bean — config-driven selection.
     *
     * <p>Mock provider her zaman mevcuttur ve default'tur.
     * OpenAI provider sadece explicit mode=openai ile aktif olur.</p>
     */
    @Bean
    @Primary
    public ImageGenerationProviderGateway imageGenerationProviderGateway(
            ImageGenerationProviderConfig config,
            @Value("${imagegen.provider.openai-api-key:}") String openAiApiKey) {

        if ("openai".equalsIgnoreCase(config.getMode())) {
            log.info("FP-007: OpenAI image generation provider aktif — mode=openai");
            return new OpenAiImageGenerationProviderGateway(config, openAiApiKey);
        }

        log.info("FP-007: Mock image generation provider aktif — mode={}", config.getMode());
        return new MockImageGenerationProviderGateway();
    }
}
