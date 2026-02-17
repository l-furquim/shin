package com.shin.commons.exception;

public final class ErrorCodes {
    
    private ErrorCodes() {
    }

    public static final String VIDEO_NOT_FOUND = "VIDEO_NOT_FOUND";
    public static final String PLAYLIST_NOT_FOUND = "PLAYLIST_NOT_FOUND";
    public static final String UPLOAD_NOT_FOUND = "UPLOAD_NOT_FOUND";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String CREATOR_NOT_FOUND = "CREATOR_NOT_FOUND";
    
    public static final String INVALID_VIDEO_REQUEST = "INVALID_VIDEO_REQUEST";
    public static final String INVALID_PLAYLIST_REQUEST = "INVALID_PLAYLIST_REQUEST";
    public static final String INVALID_VIDEO_UPLOAD = "INVALID_VIDEO_UPLOAD";
    public static final String INVALID_CHUNK = "INVALID_CHUNK";
    public static final String INVALID_ID = "INVALID_ID";
    public static final String INVALID_LOCALE = "INVALID_LOCALE";
    
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String CREATOR_ALREADY_EXISTS = "CREATOR_ALREADY_EXISTS";
    
    public static final String UNAUTHORIZED_OPERATION = "UNAUTHORIZED_OPERATION";
    
    public static final String TRANSCODER_ERROR = "TRANSCODER_ERROR";
    public static final String FFMPEG_PROCESS_ERROR = "FFMPEG_PROCESS_ERROR";
    public static final String PICTURE_UPLOAD_ERROR = "PICTURE_UPLOAD_ERROR";
    public static final String PICTURE_DELETION_ERROR = "PICTURE_DELETION_ERROR";
    
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String CONSTRAINT_VIOLATION = "CONSTRAINT_VIOLATION";
    public static final String DATABASE_ERROR = "DATABASE_ERROR";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
}
