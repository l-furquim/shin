package com.shin.metadata.handler;

import com.shin.metadata.exception.InvalidPlaylistRequestException;
import com.shin.metadata.exception.InvalidVideoRequestException;
import com.shin.metadata.exception.PlaylistNotFoundException;
import com.shin.metadata.exception.VideoNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotFound(VideoNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PlaylistNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePlaylistNotFound(PlaylistNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidVideoRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVideoRequest(InvalidVideoRequestException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidPlaylistRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPlaylistRequest(InvalidPlaylistRequestException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("An unexpected error occurred: " + ex.getMessage()));
    }
}
