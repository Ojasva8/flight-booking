package com.assessment.flightbooking.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
/**
 * Central exception handling component for the flight booking API.
 * <p>
 * Intercepts exceptions thrown by any {@code @RestController} and translates them into
 * a consistent {@link ErrorResponse} JSON body with an appropriate HTTP status code.
 * </p>
 *
 * <p>Handled exceptions and their mapped status codes:</p>
 * <ul>
 *   <li>{@link FlightNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link BookingNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link FlightFullException} → {@code 409 Conflict}</li>
 *   <li>{@link MethodArgumentNotValidException} → {@code 400 Bad Request}
 *       (Bean Validation failures on request body fields)</li>
 *   <li>{@link IllegalArgumentException} → {@code 400 Bad Request}
 *       (e.g. malformed UUID path variable)</li>
 *   <li>{@link Exception} (fallback) → {@code 500 Internal Server Error}</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler(FlightNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleFlightNotFound(FlightNotFoundException ex) {
        log.warn("FlightNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("BookingNotFoundException: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------
    @ExceptionHandler(FlightFullException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFlightFull(FlightFullException ex) {
        log.warn("FlightFullException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
    String aggregated = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", aggregated);
        return buildError(HttpStatus.BAD_REQUEST, aggregated);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Invalid value for parameter '%s'. Expected type: %s", paramName, requiredType);

        log.warn("MethodArgumentTypeMismatchException: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, message);
    }
    
    // -------------------------------------------------------------------------
    // 405 Method Not Allowed
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HttpRequestMethodNotSupportedException: {}", ex.getMessage());
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage());
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error (fallback)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ErrorResponse buildError(HttpStatus status, String message) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message
        );
    }
}
