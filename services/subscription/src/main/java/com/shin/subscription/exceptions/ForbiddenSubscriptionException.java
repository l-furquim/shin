package com.shin.subscription.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenSubscriptionException extends ForbiddenException {
    public ForbiddenSubscriptionException() {
        super(ErrorCodes.FORBIDDEN_SUBSCRIPTION, "You are not allowed to perform this subscription operation");
    }
}
