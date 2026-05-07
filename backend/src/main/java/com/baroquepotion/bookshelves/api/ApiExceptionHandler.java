package com.baroquepotion.bookshelves.api;

import com.baroquepotion.bookshelves.application.InvalidReferenceException;
import com.baroquepotion.bookshelves.application.ResourceInUseException;
import com.baroquepotion.bookshelves.application.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps application and validation failures to RFC 7807 problem responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * Returns a not-found problem when a requested resource does not exist.
     *
     * @param ex missing-resource exception raised by the application layer
     * @return problem details with HTTP 404 status
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Resource not found");
        return detail;
    }

    /**
     * Returns a bad-request problem when a request references an unknown shelf or collection.
     *
     * @param ex invalid reference exception raised during request validation
     * @return problem details with HTTP 400 status
     */
    @ExceptionHandler(InvalidReferenceException.class)
    public ProblemDetail handleInvalidReference(InvalidReferenceException ex) {
        log.warn("Invalid reference: {}", ex.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid reference");
        return detail;
    }

    /**
     * Returns a conflict problem when a reference record is still in active use.
     *
     * @param ex in-use exception raised during delete or rename checks
     * @return problem details with HTTP 409 status
     */
    @ExceptionHandler(ResourceInUseException.class)
    public ProblemDetail handleResourceInUse(ResourceInUseException ex) {
        log.warn("Resource in use: {}", ex.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Resource in use");
        return detail;
    }

    /**
     * Returns field-level validation errors for malformed request payloads.
     *
     * @param ex bean validation failure raised during request binding
     * @return problem details with HTTP 400 status and field error summaries
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Request validation failed: {}", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setTitle("Validation error");
        detail.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList());
        return detail;
    }

    /**
     * Returns a bad-request problem when an otherwise well-formed request is semantically invalid.
     *
     * @param ex invalid export or workflow request
     * @return problem details with HTTP 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid request");
        return detail;
    }

    /**
     * Returns an internal-error problem when an operational command such as export fails unexpectedly.
     *
     * @param ex internal operational failure
     * @return problem details with HTTP 500 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.error("Operation failed", ex);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        detail.setTitle("Operation failed");
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
        detail.setTitle("Internal server error");
        return detail;
    }
}
