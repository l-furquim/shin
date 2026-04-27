package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends BaseException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ForbiddenException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public ForbiddenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ForbiddenException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
