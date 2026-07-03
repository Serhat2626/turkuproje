package com.anadoluatlasi.backend.ai.gateway.mock;

/**
 * FP-004: Mock Platform Package gateway test modları.
 */
public enum MockPlatformPackageGatewayMode {

    /** Başarılı 4 platform paketi üretimi */
    SUCCESS,

    /** Provider timeout hatası */
    TIMEOUT,

    /** Boş yanıt hatası */
    EMPTY_RESPONSE,

    /** Geçersiz format hatası */
    INVALID_FORMAT,

    /** Güvenli olmayan içerik hatası */
    UNSAFE_CONTENT
}
