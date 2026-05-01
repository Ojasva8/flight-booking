package com.assessment.flightbooking.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents a flight available for booking in the system.
 * <p>
 * A {@code Flight} is uniquely identified by its {@code flightNumber} and captures
 * the route (origin → destination), schedule, and real-time seat availability.
 * This is a pure domain model with no framework dependencies.
 * </p>
 */
@Data
@Builder
public class Flight {

    /**
     * Unique identifier for the flight (e.g. "AI-101", "6E-204").
     * Acts as the natural key used to look up and reference a flight across the system.
     */
    private String flightNumber;

    /** IATA airport code or city name for the departure location (e.g. "DEL", "BOM"). */
    private String origin;

    /** IATA airport code or city name for the arrival location (e.g. "BLR", "HYD"). */
    private String destination;

    /** Scheduled date and time of departure in the local timezone of the origin airport. */
    private LocalDateTime departureTime;

    /**
     * The fixed capacity of the aircraft — this never changes once the flight is created.
     * Used as the upper bound when validating or resetting seat counts.
     */
    private int totalSeats;

    /**
     * The number of seats that can still be booked.
     * Decremented on each successful booking and incremented on cancellation.
     * Must always satisfy: {@code 0 <= availableSeats <= totalSeats}.
     */
    private int availableSeats;
}
