package com.shin.user.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BaseException;
import org.springframework.http.HttpStatus;

public class PictureUploadException extends BaseException {
    public PictureUploadException(String message) {
        super(ErrorCodes.PICTURE_UPLOAD_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
