package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;

public class UnauthorizedOperationException extends UnauthorizedException {
    public UnauthorizedOperationException(String message) {
        super(ErrorCodes.UNAUTHORIZED_OPERATION, message);
    }
}
