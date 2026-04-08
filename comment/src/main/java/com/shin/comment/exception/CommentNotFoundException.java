package com.shin.comment.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class CommentNotFoundException extends NotFoundException {
    public CommentNotFoundException() {
        super(ErrorCodes.COMMENT_NOT_FOUND, "Comment not found");
    }
}
