package com.shin.commons.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.LOCKED)
public class LockedException extends BaseException {
    public LockedException(String errorCode, String message) {
        super(errorCode, message);
    }

    public LockedException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }

    public LockedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public LockedException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }

}
