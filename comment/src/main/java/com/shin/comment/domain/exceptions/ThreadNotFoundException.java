package com.shin.comment.domain.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class ThreadNotFoundException extends NotFoundException {

    public ThreadNotFoundException() {
        super(ErrorCodes.THREAD_NOT_FOUND, "Thread not found");
    }
}
