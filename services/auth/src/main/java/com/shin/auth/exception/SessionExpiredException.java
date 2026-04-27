package com.shin.auth.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class SessionExpiredException extends UnauthorizedException {
    public SessionExpiredException() {
        super(ErrorCodes.SESSION_EXPIRED, "Session expired");
    }
}
