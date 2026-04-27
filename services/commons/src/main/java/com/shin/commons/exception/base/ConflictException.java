package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends BaseException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public ConflictException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ConflictException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
