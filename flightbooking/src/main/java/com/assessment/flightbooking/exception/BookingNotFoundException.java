package com.assessment.flightbooking.exception;

import java.util.UUID;

/**
 * Thrown when a booking with the requested {@link UUID} does not exist in the system.
 * <p>
 * Raised by the service layer on any operation (cancellation, lookup) that targets a
 * booking ID that is not present in the {@code BookingRepository}.
 * </p>
 */
public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found: " + bookingId);
    }
}
