package com.anadoluatlasi.backend.ai.contract;

import lombok.Builder;
import lombok.Value;

/**
 * FP-004: Platform paketi hedef input'u.
 */
@Value
@Builder
public class PlatformPackageTargetInput {

    /** Platform tipi adı (ör. "YOUTUBE_LONG_FORM") */
    String platformType;

    /** Platform-spesifik not / talimat */
    String platformNote;
}
