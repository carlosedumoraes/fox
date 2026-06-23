package com.fox.exception;

public class AccessDeniedApiException extends RuntimeException {
    public AccessDeniedApiException(String message) {
        super(message);
    }
}
