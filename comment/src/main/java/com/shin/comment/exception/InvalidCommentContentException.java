package com.shin.comment.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;

public class InvalidCommentContentException extends UnauthorizedException {
    public InvalidCommentContentException() {
        super(ErrorCodes.INVALID_COMMENT_CONTENT, "Invalid comment content.");
    }
}
