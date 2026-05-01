package com.assessment.flightbooking.exception;

/**
 * Thrown when a booking is attempted on a flight that has no remaining available seats.
 * <p>
 * Raised by the service layer after confirming the flight exists but its
 * {@code availableSeats} count has reached zero. Signals that the caller should
 * not retry without first waiting for a cancellation to free a seat.
 * </p>
 */
public class FlightFullException extends RuntimeException {

    public FlightFullException(String flightNumber) {
        super("Flight " + flightNumber + " is fully booked");
    }
}
