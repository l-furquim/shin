package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BadRequestException;

public class InvalidPlaylistRequestException extends BadRequestException {
    public InvalidPlaylistRequestException(String message) {
        super(ErrorCodes.INVALID_PLAYLIST_REQUEST, message);
    }
}
