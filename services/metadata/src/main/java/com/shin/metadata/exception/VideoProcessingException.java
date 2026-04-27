package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.LockedException;

public class VideoProcessingException extends LockedException {
    public VideoProcessingException() {
        super(
                ErrorCodes.VIDEO_STILL_PROCESSING, "Video still processing"
        );
    }
}
