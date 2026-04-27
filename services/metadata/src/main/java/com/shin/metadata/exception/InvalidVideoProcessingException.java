package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidVideoProcessingException extends BadRequestException {
    public InvalidVideoProcessingException() {
        super(ErrorCodes.INVALID_VIDEO_REQUEST, "Invalid video processing data");
    }
}
