package com.chrionline.core.exceptions;

/**
 * Exception lancée lors d'une violation d'une règle métier.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
