package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ForbiddenException;

public class ForbiddenVideoOperationException extends ForbiddenException {
    public ForbiddenVideoOperationException() {
        super(ErrorCodes.FORBIDDEN_VIDEO_OPERATION, ErrorCodes.FORBIDDEN_VIDEO_OPERATION);
    }
}
