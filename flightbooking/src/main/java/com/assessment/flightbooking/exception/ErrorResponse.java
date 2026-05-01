package com.assessment.flightbooking.exception;

/**
 * Uniform error payload returned by {@link GlobalExceptionHandler} for all error responses.
 * <p>
 * Serialised as JSON and included in the response body for any non-2xx outcome.
 * The {@code timestamp} field is always an ISO-8601 string so clients can parse it
 * without locale-specific date formatters.
 * </p>
 *
 * @param timestamp ISO-8601 UTC timestamp of when the error was generated
 *                  (e.g. {@code "2024-06-01T10:30:00"})
 * @param status    HTTP status code (e.g. {@code 404}, {@code 409}, {@code 500})
 * @param error     short HTTP reason phrase matching the status (e.g. {@code "Not Found"})
 * @param message   human-readable description of what went wrong; may be a validation
 *                  summary or a business error message
 */
public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message
) {}
