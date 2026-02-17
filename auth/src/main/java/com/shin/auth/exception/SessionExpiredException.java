package com.shin.auth.exception;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException() {
        super("Session expired");
    }
}
