package com.anadoluatlasi.backend.common.exception;

/**
 * Validasyon hatalarında fırlatılan exception.
 * HTTP 400 yanıtı döndürülür.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message);
    }
}
