package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PictureDeletionException extends InternalException {
    public PictureDeletionException(String message) {
        super(ErrorCodes.PICTURE_DELETION_ERROR, message);
    }
}
