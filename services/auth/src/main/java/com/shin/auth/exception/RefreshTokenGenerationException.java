package com.shin.auth.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RefreshTokenGenerationException extends InternalException {
    public RefreshTokenGenerationException(String message) {
        super(ErrorCodes.REFRESH_TOKEN_GENERATION_ERROR, message);
    }
}
