package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class VideoProcessingNotFound extends NotFoundException {
    public VideoProcessingNotFound() {
        super(ErrorCodes.VIDEO_NOT_FOUND, "Video processing not found");
    }
}
