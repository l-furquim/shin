package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidThumbnailUploadException extends BadRequestException {
    public InvalidThumbnailUploadException(String message) {
        super(ErrorCodes.INVALID_VIDEO_UPLOAD, message);
    }
}
