package com.anadoluatlasi.backend.imagegen.provider;

import com.anadoluatlasi.backend.imagegen.entity.ImageGenerationErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FP-007: Normalized provider generation result model.
 *
 * <p>Provider'dan dönen sonucun güvenli normalize edilmiş hali.
 * Raw provider response, raw provider request, signed URL,
 * public URL, image binary, base64, full prompt içermez.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageProviderGenerationResult {
    private boolean success;
    private String providerAlias;
    private String modelName;
    private Integer outputCount;
    private Long generationDurationMs;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSizeBytes;
    private String checksumSha256;
    private ImageGenerationErrorCode errorCode;
    private String errorMessage;
    private UUID correlationId;
    private LocalDateTime completedAt;
    private String qualitySignalsSummary;
    private String safetyFlagsSummary;
}
