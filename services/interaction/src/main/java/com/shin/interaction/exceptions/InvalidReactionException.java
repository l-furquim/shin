package com.shin.interaction.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;

public class InvalidReactionException extends UnauthorizedException {
    public InvalidReactionException() {
        super(ErrorCodes.VALIDATION_FAILED, "Provide a valid video id");
    }
}
