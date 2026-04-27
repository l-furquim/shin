package com.shin.streaming.exception;


import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;

public class PlaybackEventProducerException extends InternalException {
    public PlaybackEventProducerException() {
        super(ErrorCodes.INTERNAL_SERVER_ERROR, "Error while sending event, retry later");
    }
}
