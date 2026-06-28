package com.anadoluatlasi.backend.common.exception;

/**
 * İş kuralı ihlallerinde fırlatılan temel exception sınıfı.
 * Tüm domain-spesifik exception'lar bu sınıftan türer.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
