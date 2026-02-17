package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ConflictException;

public class DuplicatedEmailException extends ConflictException {
    public DuplicatedEmailException(String message) {
        super(ErrorCodes.EMAIL_ALREADY_EXISTS, message);
    }
}
