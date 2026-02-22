package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidLikeException extends BadRequestException {
    public InvalidLikeException(String message) {
        super(ErrorCodes.INVALID_LIKE, message);
    }
}
