package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ConflictException;

public class DuplicatedCreatorException extends ConflictException {
    public DuplicatedCreatorException(String message) {
        super(ErrorCodes.CREATOR_ALREADY_EXISTS, message);
    }
}
