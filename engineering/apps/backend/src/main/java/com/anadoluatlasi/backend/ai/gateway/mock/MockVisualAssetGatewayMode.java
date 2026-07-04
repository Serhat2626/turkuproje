package com.anadoluatlasi.backend.ai.gateway.mock;

/**
 * FP-006: Mock Visual Asset gateway çalışma modu.
 *
 * <p>Test senaryolarını simüle etmek için kullanılır.</p>
 */
public enum MockVisualAssetGatewayMode {
    SUCCESS,
    TIMEOUT,
    EMPTY_RESPONSE,
    INVALID_FORMAT,
    UNSAFE_CONTENT
}
