package com.chrionline.core.exceptions;

/**
 * Exception lancée lorsqu'une validation métier échoue.
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
