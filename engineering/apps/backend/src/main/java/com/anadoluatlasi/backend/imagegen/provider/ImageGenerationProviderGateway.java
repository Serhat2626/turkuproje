package com.anadoluatlasi.backend.imagegen.provider;

/**
 * FP-007: Provider-agnostic image generation gateway interface.
 *
 * <p>Tüm image generation provider'ları bu interface'i implement eder.
 * Gateway safe input alır, normalized result döner.</p>
 *
 * <p>Bu interface asla:</p>
 * <ul>
 *   <li>Raw provider payload/response expose etmez</li>
 *   <li>API key/secret taşımaz</li>
 *   <li>Image binary/base64 döndürmez</li>
 *   <li>Storage upload/download yapmaz</li>
 *   <li>Public/signed URL üretmez</li>
 *   <li>GeneratedAsset/MediaAsset oluşturmaz</li>
 * </ul>
 */
public interface ImageGenerationProviderGateway {

    /**
     * Provider'a generation request gönderir, normalized result döner.
     *
     * @param request provider-agnostic safe request
     * @return normalized generation result (success veya failure)
     */
    ImageProviderGenerationResult generate(ImageProviderGenerationRequest request);

    /**
     * Provider alias — log/metadata için kullanılır.
     * Örn: "MOCK", "OPENAI", "REPLICATE"
     */
    String getProviderAlias();
}
