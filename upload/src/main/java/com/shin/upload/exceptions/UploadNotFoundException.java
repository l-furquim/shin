package com.shin.upload.exceptions;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.NotFoundException;

public class UploadNotFoundException extends NotFoundException {
    public UploadNotFoundException(String message) {
        super(ErrorCodes.UPLOAD_NOT_FOUND, message);
    }
}
