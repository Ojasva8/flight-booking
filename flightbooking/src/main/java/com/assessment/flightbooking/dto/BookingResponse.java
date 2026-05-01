package com.assessment.flightbooking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO representing a successfully created booking.
 * <p>
 * Returned in the response body of the {@code POST /api/v1/bookings} endpoint.
 * Contains all fields a client needs to reference or display the booking:
 * its unique ID, the associated flight, passenger details, and creation timestamp.
 * </p>
 *
 * @param bookingId      the globally unique identifier assigned to this booking
 * @param flightNumber   the flight number this booking is associated with
 * @param passengerName  full name of the passenger
 * @param passengerEmail contact email of the passenger
 * @param bookedAt       the UTC timestamp at which the booking was created
 */
public record BookingResponse(
        UUID bookingId,
        String flightNumber,
        String passengerName,
        String passengerEmail,
        LocalDateTime bookedAt
) {}
