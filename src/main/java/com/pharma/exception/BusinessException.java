package com.pharma.exception;

/** Thrown for domain rule violations, e.g. insufficient stock. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
