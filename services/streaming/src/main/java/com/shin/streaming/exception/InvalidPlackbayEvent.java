package com.shin.streaming.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidPlackbayEvent extends BadRequestException {
    public InvalidPlackbayEvent() {
        super(ErrorCodes.INVALID_VIDEO_UPLOAD, "Invalid playback payload for this video");
    }
}
