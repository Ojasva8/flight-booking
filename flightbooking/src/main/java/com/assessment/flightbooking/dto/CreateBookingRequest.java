package com.assessment.flightbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO carrying the data required to create a new flight booking.
 * <p>
 * Validated on receipt by the controller using Jakarta Bean Validation.
 * All fields are mandatory; an invalid or missing value results in a 400 Bad Request
 * before the request reaches the service layer.
 * </p>
 *
 * @param flightNumber   the unique identifier of the flight to book (must not be blank)
 * @param passengerName  full name of the passenger (must not be blank)
 * @param passengerEmail contact email of the passenger (must be a well-formed email address)
 */
public record CreateBookingRequest(

        @NotBlank(message = "Flight number must not be blank")
        String flightNumber,

        @NotBlank(message = "Passenger name must not be blank")
        String passengerName,

        @NotBlank(message = "Passenger email must not be blank")
        @Email(message = "Passenger email must be a valid email address")
        String passengerEmail
) {}
