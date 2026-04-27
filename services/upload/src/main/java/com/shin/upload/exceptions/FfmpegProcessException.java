package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class FfmpegProcessException extends InternalException {
    public FfmpegProcessException(String message) {
        super(ErrorCodes.FFMPEG_PROCESS_ERROR, message);
    }
}
