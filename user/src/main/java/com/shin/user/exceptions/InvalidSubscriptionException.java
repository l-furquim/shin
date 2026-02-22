package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidSubscriptionException extends BadRequestException {
    public InvalidSubscriptionException(String message) {
        super(ErrorCodes.INVALID_SUBSCRIPTION, message);
    }
}
