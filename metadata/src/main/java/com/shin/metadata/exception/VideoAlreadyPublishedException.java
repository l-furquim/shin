package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.ConflictException;

public class VideoAlreadyPublishedException extends ConflictException {
    public VideoAlreadyPublishedException() {
        super(ErrorCodes.FORBIDDEN_VIDEO_OPERATION, "Video already published");
    }
}
