package com.assessment.flightbooking.exception;

/**
 * Thrown when a flight with the requested flight number does not exist in the system.
 * <p>
 * Raised by the service layer on any operation (booking, lookup) that targets a
 * flight number that is not present in the {@code FlightRepository}.
 * </p>
 */
public class FlightNotFoundException extends RuntimeException {

    public FlightNotFoundException(String flightNumber) {
        super("Flight not found: " + flightNumber);
    }
}
