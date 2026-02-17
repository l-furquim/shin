package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidVideoUploadException extends BadRequestException {
    public InvalidVideoUploadException(String message) {
        super(ErrorCodes.INVALID_VIDEO_UPLOAD, message);
    }
}
