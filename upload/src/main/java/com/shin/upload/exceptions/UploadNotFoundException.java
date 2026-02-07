package com.shin.upload.exceptions;

public class UploadNotFoundException extends RuntimeException {
    public UploadNotFoundException(String message) {
        super(message);
    }
}
