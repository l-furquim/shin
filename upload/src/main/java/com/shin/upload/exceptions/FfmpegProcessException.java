package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class FfmpegProcessException extends BaseException {
    public FfmpegProcessException(String message) {
        super(ErrorCodes.FFMPEG_PROCESS_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
