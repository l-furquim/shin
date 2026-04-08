package com.shin.comment.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ForbiddenException;

public class ForbiddenCommentOperationException extends ForbiddenException {
    public ForbiddenCommentOperationException() {
        super(ErrorCodes.UNAUTHORIZED_OPERATION, "You dont have access to this resource");
    }
}
