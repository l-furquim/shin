package com.shin.search.exception.handler;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.InternalException;

public class MetadataFetchException extends InternalException {
    public MetadataFetchException() {
        super(ErrorCodes.INTERNAL_SERVER_ERROR, "Could not fetch videos, please, try again later");
    }
}
