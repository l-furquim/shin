package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalException extends BaseException {

    public InternalException(String errorCode, String message) {
        super(errorCode, message);
    }

    public InternalException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public InternalException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public InternalException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
}
