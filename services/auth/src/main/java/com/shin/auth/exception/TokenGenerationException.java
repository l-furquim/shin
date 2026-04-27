package com.shin.auth.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TokenGenerationException extends InternalException {
    public TokenGenerationException(String message) {
        super(ErrorCodes.TOKEN_GENERATION_ERROR, message);
    }
}
