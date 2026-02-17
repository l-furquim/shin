package com.shin.auth.exception;

public class RefreshTokenGenerationException extends RuntimeException {
    public RefreshTokenGenerationException(String message) {
        super(message);
    }
}
