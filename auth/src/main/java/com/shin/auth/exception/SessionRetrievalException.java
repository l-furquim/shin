package com.shin.auth.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SessionRetrievalException extends InternalException {
    public SessionRetrievalException(String message) {
        super(ErrorCodes.SESSION_RETRIEVAL_ERROR, message);
    }
}
