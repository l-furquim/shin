package com.shin.upload.exceptions;

public class InvalidChunkException extends RuntimeException {
    public InvalidChunkException(String message) {
        super(message);
    }
}
