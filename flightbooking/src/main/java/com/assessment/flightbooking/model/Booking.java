package com.assessment.flightbooking.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a confirmed seat reservation made by a passenger for a specific flight.
 * <p>
 * A {@code Booking} ties a passenger's identity to a {@link Flight} via {@code flightNumber}
 * and records the exact moment the reservation was created. Each booking is assigned a
 * globally unique identifier ({@link UUID}) at creation time to allow safe lookups and
 * cancellations without relying on mutable state.
 * </p>
 */
@Data
@Builder
public class Booking {

    /**
     * Globally unique identifier for this booking record.
     * Generated via {@link UUID#randomUUID()} at the time the booking is created;
     * never reused or reassigned.
     */
    private UUID bookingId;

    /**
     * References the flight this booking belongs to.
     * Matches the {@code flightNumber} field in {@link Flight} — acts as the foreign key
     * linking a booking to its flight without embedding the full {@code Flight} object.
     */
    private String flightNumber;

    private String passengerName;

    /** Contact email for the passenger; used for booking confirmation and notifications. */
    private String passengerEmail;

    /**
     * The UTC timestamp of when this booking was created.
     * Set to {@link LocalDateTime#now()} at booking time and treated as immutable thereafter.
     */
    private LocalDateTime bookedAt;
}
