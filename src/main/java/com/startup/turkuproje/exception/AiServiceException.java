package com.startup.turkuproje.exception;

// AI servisi çağrılarında oluşan hatalar için
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
