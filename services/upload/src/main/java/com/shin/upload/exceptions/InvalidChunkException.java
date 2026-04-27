package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidChunkException extends BadRequestException {
    public InvalidChunkException(String message) {
        super(ErrorCodes.INVALID_CHUNK, message);
    }
}
