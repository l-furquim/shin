package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public UnauthorizedException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public UnauthorizedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public UnauthorizedException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
