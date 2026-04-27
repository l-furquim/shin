package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PictureUploadException extends InternalException {
    public PictureUploadException(String message) {
        super(ErrorCodes.PICTURE_UPLOAD_ERROR, message);
    }
}
