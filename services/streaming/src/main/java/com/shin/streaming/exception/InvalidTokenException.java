package com.shin.streaming.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ForbiddenException;

public class InvalidTokenException extends ForbiddenException {
    public InvalidTokenException() {
        super(ErrorCodes.INVALID_TOKEN, "Invalid playback session token");
    }
}
