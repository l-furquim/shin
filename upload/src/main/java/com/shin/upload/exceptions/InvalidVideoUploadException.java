package com.shin.upload.exceptions;

public class InvalidVideoUploadException extends RuntimeException {
    public InvalidVideoUploadException(String message) {
        super(message);
    }
}
