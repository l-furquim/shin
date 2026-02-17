package com.shin.metadata.exception;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class PlaylistNotFoundException extends NotFoundException {
    public PlaylistNotFoundException(String message) {
        super(ErrorCodes.PLAYLIST_NOT_FOUND, message);
    }
}
