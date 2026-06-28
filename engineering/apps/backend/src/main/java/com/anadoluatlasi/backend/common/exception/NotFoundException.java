package com.anadoluatlasi.backend.common.exception;

/**
 * Kaynak bulunamadığında fırlatılan exception.
 * HTTP 404 yanıtı döndürülür.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String entityName, Object id) {
        super(String.format("%s bulunamadı: %s", entityName, id));
    }
}
