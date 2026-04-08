package com.shin.comment.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidCommentException extends BadRequestException {
    public InvalidCommentException() {
        super(ErrorCodes.VALIDATION_FAILED, "Invalid comment data");
    }
}
