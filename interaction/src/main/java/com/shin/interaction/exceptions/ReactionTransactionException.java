package com.shin.interaction.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.UnauthorizedException;

public class ReactionTransactionException extends UnauthorizedException {
    public ReactionTransactionException() {
        super(ErrorCodes.IDEMPOTENCY_ERROR, "Operação invalida");
    }
}
