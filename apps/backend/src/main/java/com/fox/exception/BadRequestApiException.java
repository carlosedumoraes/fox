package com.fox.exception;

public class BadRequestApiException extends RuntimeException {
    public BadRequestApiException(String message) {
        super(message);
    }
}
