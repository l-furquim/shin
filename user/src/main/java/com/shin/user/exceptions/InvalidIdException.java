package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidIdException extends BadRequestException {
    public InvalidIdException(String message) {
        super(ErrorCodes.INVALID_ID, message);
    }
}
