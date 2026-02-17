package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidVideoRequestException extends BadRequestException {
    public InvalidVideoRequestException(String message) {
        super(ErrorCodes.INVALID_VIDEO_REQUEST, message);
    }
}
