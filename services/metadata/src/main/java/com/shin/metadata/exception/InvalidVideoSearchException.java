package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidVideoSearchException extends BadRequestException {
    public InvalidVideoSearchException(String message) {
        super(ErrorCodes.INVALID_SEARCH_REQUEST, message);
    }
}
