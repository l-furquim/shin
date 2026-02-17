package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends BaseException {

    public NotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    public NotFoundException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public NotFoundException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public NotFoundException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
