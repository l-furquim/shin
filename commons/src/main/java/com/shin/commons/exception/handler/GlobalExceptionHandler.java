package com.shin.commons.exception.handler;

import com.shin.commons.exception.ErrorCodes;
import com.shin.commons.exception.base.BaseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetail> handleBaseException(
        BaseException ex,
        HttpServletRequest request
    ) {
        HttpStatus status = determineHttpStatus(ex);
        String correlationId = getCorrelationId();
        
        log.warn("Business exception occurred [correlationId={}, errorCode={}]: {}", 
            correlationId, ex.getErrorCode(), ex.getMessage());
        
        ProblemDetail problemDetail = createProblemDetail(
            status,
            ex.getErrorCode(),
            formatTitle(ex.getErrorCode()),
            ex.getMessage(),
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(status).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Validation failed [correlationId={}]: {} errors", 
            correlationId, ex.getBindingResult().getErrorCount());
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Validation Failed",
            "Request validation failed. Please check the errors and try again.",
            request.getRequestURI(),
            correlationId
        );
        
        addValidationErrors(problemDetail, ex.getBindingResult().getFieldErrors());
        
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Constraint violation [correlationId={}]: {} violations", 
            correlationId, ex.getConstraintViolations().size());
        
        Map<String, String> violations = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (v1, v2) -> v1
            ));
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.CONSTRAINT_VIOLATION,
            "Constraint Violation",
            "Request validation failed due to constraint violations.",
            request.getRequestURI(),
            correlationId
        );
        
        addConstraintViolations(problemDetail, violations);
        
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Malformed JSON request [correlationId={}]", correlationId);
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Malformed Request",
            "Request body is malformed or cannot be read. Please check your JSON syntax.",
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Unsupported media type [correlationId={}]: {}", correlationId, ex.getContentType());
        
        String supportedTypes = ex.getSupportedMediaTypes().stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "Unsupported Media Type",
            "Media type '" + ex.getContentType() + "' is not supported. Supported types: " + supportedTypes,
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problemDetail);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleHttpRequestMethodNotSupported(
        HttpRequestMethodNotSupportedException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Method not allowed [correlationId={}]: {}", correlationId, ex.getMethod());
        
        String supportedMethods = ex.getSupportedHttpMethods() != null 
            ? ex.getSupportedHttpMethods().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "))
            : "N/A";
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.METHOD_NOT_ALLOWED,
            "METHOD_NOT_ALLOWED",
            "Method Not Allowed",
            "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint. Supported methods: " + supportedMethods,
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
        MethodArgumentTypeMismatchException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Type mismatch [correlationId={}]: parameter '{}' expected type {}", 
            correlationId, ex.getName(), ex.getRequiredType());
        
        String message = String.format(
            "Parameter '%s' has invalid value '%s'. Expected type: %s",
            ex.getName(),
            ex.getValue(),
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.BAD_REQUEST,
            ErrorCodes.VALIDATION_FAILED,
            "Invalid Parameter Type",
            message,
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.badRequest().body(problemDetail);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoHandlerFoundException(
        NoHandlerFoundException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("No handler found [correlationId={}]: {} {}", 
            correlationId, ex.getHttpMethod(), ex.getRequestURL());
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.NOT_FOUND,
            "ENDPOINT_NOT_FOUND",
            "Endpoint Not Found",
            "The requested endpoint does not exist.",
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
        DataIntegrityViolationException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.error("Data integrity violation [correlationId={}]", correlationId, ex);
        
        String userMessage = extractUserFriendlyDatabaseMessage(ex);
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            ErrorCodes.DATABASE_ERROR,
            "Data Integrity Violation",
            userMessage,
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(
        OptimisticLockingFailureException ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.warn("Optimistic locking failure [correlationId={}]", correlationId);
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.CONFLICT,
            ErrorCodes.CONCURRENT_MODIFICATION,
            "Concurrent Modification",
            "The resource was modified by another user. Please refresh and try again.",
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        String correlationId = getCorrelationId();
        
        log.error("Unexpected error occurred [correlationId={}]", correlationId, ex);
        
        ProblemDetail problemDetail = createProblemDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCodes.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred. Please try again later or contact support if the problem persists.",
            request.getRequestURI(),
            correlationId
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private ProblemDetail createProblemDetail(
        HttpStatus status,
        String errorCode,
        String title,
        String detail,
        String instance,
        String correlationId
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create("https://api.shin.com/errors/" + toKebabCase(errorCode)));
        problemDetail.setTitle(title);
        
        if (instance != null) {
            problemDetail.setInstance(URI.create(instance));
        }
        
        problemDetail.setProperty("timestamp", Instant.now().toString());
        
        if (correlationId != null && !correlationId.isBlank()) {
            problemDetail.setProperty("correlationId", correlationId);
        }
        
        return problemDetail;
    }

    private void addValidationErrors(ProblemDetail problemDetail, List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return;
        }

        List<Map<String, String>> errors = fieldErrors.stream()
            .map(error -> {
                Map<String, String> errorMap = new HashMap<>();
                errorMap.put("field", error.getField());
                errorMap.put("message", error.getDefaultMessage());
                return errorMap;
            })
            .toList();

        problemDetail.setProperty("errors", errors);
    }

    private void addConstraintViolations(ProblemDetail problemDetail, Map<String, String> violations) {
        if (violations == null || violations.isEmpty()) {
            return;
        }

        List<Map<String, String>> errors = violations.entrySet().stream()
            .map(entry -> Map.of(
                "field", entry.getKey(),
                "message", entry.getValue()
            ))
            .toList();

        problemDetail.setProperty("errors", errors);
    }

    private String getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    private HttpStatus determineHttpStatus(BaseException ex) {
        try {
            Class<? extends BaseException> exceptionClass = ex.getClass();
            if (exceptionClass.isAnnotationPresent(org.springframework.web.bind.annotation.ResponseStatus.class)) {
                return exceptionClass.getAnnotation(org.springframework.web.bind.annotation.ResponseStatus.class).value();
            }
        } catch (Exception e) {
            log.debug("Could not determine HTTP status from annotation", e);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String formatTitle(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return "Error";
        }
        
        String[] words = errorCode.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }

    private String toKebabCase(String input) {
        if (input == null || input.isBlank()) {
            return "unknown-error";
        }
        
        return input.toLowerCase()
            .replaceAll("_", "-")
            .replaceAll("\\s+", "-");
    }

    private String extractUserFriendlyDatabaseMessage(DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        
        if (message == null) {
            return "A database constraint was violated. Please check your data and try again.";
        }
        
        if (message.contains("unique constraint") || message.contains("UNIQUE constraint")) {
            return "A record with this value already exists. Please use a different value.";
        }
        
        if (message.contains("foreign key constraint") || message.contains("FOREIGN KEY constraint")) {
            return "The operation cannot be completed because it references data that does not exist.";
        }
        
        if (message.contains("not-null constraint") || message.contains("NOT NULL constraint")) {
            return "A required field is missing. Please provide all required information.";
        }
        
        return "A database constraint was violated. Please check your data and try again.";
    }
}
