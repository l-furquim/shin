package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class TranscoderProducerException extends BaseException {
    public TranscoderProducerException(String message) {
        super(ErrorCodes.TRANSCODER_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
