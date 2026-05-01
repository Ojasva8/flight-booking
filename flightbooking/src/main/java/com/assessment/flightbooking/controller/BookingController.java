package com.assessment.flightbooking.controller;

import com.assessment.flightbooking.dto.BookingResponse;
import com.assessment.flightbooking.dto.CreateBookingRequest;
import com.assessment.flightbooking.model.Booking;
import com.assessment.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing the flight booking API under {@code /api/v1/bookings}.
 * <p>
 * Provides two operations:
 * <ul>
 *   <li>{@code POST /api/v1/bookings} — create a new booking</li>
 *   <li>{@code DELETE /api/v1/bookings/{bookingId}} — cancel an existing booking</li>
 * </ul>
 * Request validation is delegated to Jakarta Bean Validation via {@code @Valid}.
 * Business logic is delegated entirely to {@link BookingService}; this class contains
 * no domain logic beyond mapping between DTOs and domain objects.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Creates a new seat reservation on the specified flight.
     *
     * <p><b>HTTP contract:</b></p>
     * <ul>
     *   <li>Method: {@code POST}</li>
     *   <li>Path: {@code /api/v1/bookings}</li>
     *   <li>Request body: {@link CreateBookingRequest} (JSON, validated)</li>
     *   <li>Response: {@code 201 Created} with {@link BookingResponse} body on success</li>
     *   <li>{@code 400 Bad Request} if the request body fails validation</li>
     *   <li>{@code 404 Not Found} if the flight number does not exist</li>
     *   <li>{@code 409 Conflict} if the flight is fully booked</li>
     * </ul>
     *
     * @param request validated inbound booking request
     * @return the created booking as a {@link BookingResponse}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        log.info("POST /api/v1/bookings - request received for flight {}", request.flightNumber());

        Booking booking = bookingService.createBooking(
                request.flightNumber(),
                request.passengerName(),
                request.passengerEmail()
        );

        return toResponse(booking);
    }

    /**
     * Cancels an existing booking and releases the seat back to the flight.
     *
     * <p><b>HTTP contract:</b></p>
     * <ul>
     *   <li>Method: {@code DELETE}</li>
     *   <li>Path: {@code /api/v1/bookings/{bookingId}}</li>
     *   <li>Path variable: {@code bookingId} — UUID of the booking to cancel</li>
     *   <li>Response: {@code 204 No Content} on success (no body)</li>
     *   <li>{@code 404 Not Found} if no booking exists with the given ID</li>
     * </ul>
     *
     * @param bookingId UUID of the booking to cancel
     */
    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@PathVariable UUID bookingId) {
        log.info("DELETE /api/v1/bookings/{} - cancellation request received", bookingId);
        bookingService.cancelBooking(bookingId);
    }

    /**
     * Maps a {@link Booking} domain object to a {@link BookingResponse} DTO.
     * Kept private to enforce that the mapping is always done through this single method.
     *
     * @param booking the domain booking to map
     * @return the corresponding response DTO
     */
    private BookingResponse toResponse(Booking booking) {
        log.debug("Mapping booking {} to response DTO", booking.getBookingId());
        return new BookingResponse(
                booking.getBookingId(),
                booking.getFlightNumber(),
                booking.getPassengerName(),
                booking.getPassengerEmail(),
                booking.getBookedAt()
        );
    }
}
