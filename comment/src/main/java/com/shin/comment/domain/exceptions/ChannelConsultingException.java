package com.shin.comment.domain.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;

public class ChannelConsultingException extends InternalException {
    public ChannelConsultingException() {
        super(ErrorCodes.INTERNAL_SERVER_ERROR, "Error while consuting the channel informations, try again later.");
    }
}
