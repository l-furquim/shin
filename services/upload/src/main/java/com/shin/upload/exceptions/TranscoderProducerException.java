package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TranscoderProducerException extends InternalException {
    public TranscoderProducerException(String message) {
        super(ErrorCodes.TRANSCODER_ERROR, message);
    }
}
