package com.shin.subscription.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SubscriptionError extends InternalException {
    public SubscriptionError() {
        super(ErrorCodes.SUBSCRIPTION_ERROR, "Error while subscribing, please try again later");
    }
}
