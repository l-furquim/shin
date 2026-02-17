package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class PictureDeletionException extends BaseException {
    public PictureDeletionException(String message) {
        super(ErrorCodes.PICTURE_DELETION_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
