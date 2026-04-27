package com.shin.auth.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends UnauthorizedException {
    public InvalidCredentialsException(String message) {
        super(ErrorCodes.INVALID_CREDENTIALS, message);
    }
}
