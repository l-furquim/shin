package com.shin.streaming.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ForbiddenException;

public class VideoAccessDeniedException extends ForbiddenException {

    public VideoAccessDeniedException(String message) {
        super(ErrorCodes.UNAUTHORIZED_OPERATION, message);
    }
}
