package com.shin.streaming.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidWatchVodRequest extends BadRequestException {
    public InvalidWatchVodRequest() {
        super(ErrorCodes.INVALID_VIDEO_UPLOAD, "Invalid vod request");
    }
}
