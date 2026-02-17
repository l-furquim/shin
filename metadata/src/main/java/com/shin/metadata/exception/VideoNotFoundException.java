package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class VideoNotFoundException extends NotFoundException {
    public VideoNotFoundException(String message) {
        super(ErrorCodes.VIDEO_NOT_FOUND, message);
    }
}
