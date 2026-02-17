package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidLocaleException extends BadRequestException {
    public InvalidLocaleException(String message) {
        super(ErrorCodes.INVALID_LOCALE, message);
    }
}
