package com.assessment.flightbooking.service;

import com.assessment.flightbooking.exception.BookingNotFoundException;
import com.assessment.flightbooking.exception.FlightFullException;
import com.assessment.flightbooking.exception.FlightNotFoundException;
import com.assessment.flightbooking.model.Booking;
import com.assessment.flightbooking.model.Flight;
import com.assessment.flightbooking.repository.BookingRepository;
import com.assessment.flightbooking.repository.FlightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core business logic for the flight booking system.
 * <p>
 * Orchestrates interactions between {@link FlightRepository} and {@link BookingRepository}
 * to create and cancel seat reservations. Enforces business invariants such as seat
 * availability and uses fine-grained synchronization on individual {@link Flight} instances
 * to prevent overbooking under concurrent HTTP requests.
 * </p>
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final FlightRepository flightRepository;
    private final BookingRepository bookingRepository;

    public BookingService(FlightRepository flightRepository, BookingRepository bookingRepository) {
        this.flightRepository = flightRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Creates a new seat reservation for the given passenger on the specified flight.
     * <p>
     * The method checks that the flight exists and has at least one available seat before
     * decrementing the count and persisting the booking. Seat decrement is performed inside
     * a {@code synchronized} block to ensure correctness under concurrent requests.
     * </p>
     *
     * @param flightNumber   the unique identifier of the flight to book
     * @param passengerName  full name of the passenger
     * @param passengerEmail contact email of the passenger
     * @return the persisted {@link Booking} with its generated ID and timestamp
     * @throws FlightNotFoundException if no flight with {@code flightNumber} exists
     * @throws FlightFullException     if the flight has no remaining available seats
     */
    public Booking createBooking(String flightNumber, String passengerName, String passengerEmail) {
        log.info("Creating booking for flight {} for passenger {}", flightNumber, passengerName);

        Flight flight = flightRepository.findByFlightNumber(flightNumber)
                .orElseGet(() -> {
                    log.warn("Flight not found: {}", flightNumber);
                    throw new FlightNotFoundException(flightNumber);
                });

        // Synchronize on the specific Flight instance to prevent two concurrent threads from
        // both reading availableSeats > 0, both decrementing, and thus overboooking the flight.
        synchronized (flight) {
            if (flight.getAvailableSeats() <= 0) {
                log.warn("Attempt to book fully booked flight {}", flightNumber);
                throw new FlightFullException(flightNumber);
            }
            flight.setAvailableSeats(flight.getAvailableSeats() - 1);
            flightRepository.save(flight);
        }

        Booking booking = Booking.builder()
                .bookingId(UUID.randomUUID())
                .flightNumber(flightNumber)
                .passengerName(passengerName)
                .passengerEmail(passengerEmail)
                .bookedAt(LocalDateTime.now())
                .build();

        bookingRepository.save(booking);
        log.info("Booking {} created successfully for flight {}", booking.getBookingId(), flightNumber);
        return booking;
    }

    /**
     * Cancels an existing booking and restores the seat to the associated flight.
     * <p>
     * The method looks up the booking and its flight, increments the available seat count
     * inside a {@code synchronized} block (matching the same guard used in
     * {@link #createBooking}), then removes the booking record.
     * </p>
     *
     * @param bookingId the UUID of the booking to cancel
     * @throws BookingNotFoundException if no booking with {@code bookingId} exists
     * @throws FlightNotFoundException  if the flight referenced by the booking no longer exists
     */
    public void cancelBooking(UUID bookingId) {
        log.info("Cancelling booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseGet(() -> {
                    log.warn("Booking not found: {}", bookingId);
                    throw new BookingNotFoundException(bookingId);
                });

        Flight flight = flightRepository.findByFlightNumber(booking.getFlightNumber())
                .orElseGet(() -> {
                    log.warn("Flight not found: {}", booking.getFlightNumber());
                    throw new FlightNotFoundException(booking.getFlightNumber());
                });

        // Synchronize on the Flight instance so that seat restoration is atomic with respect
        // to any concurrent createBooking calls targeting the same flight.
        synchronized (flight) {
            flight.setAvailableSeats(flight.getAvailableSeats() + 1);
            flightRepository.save(flight);
        }

        bookingRepository.deleteById(bookingId);
        log.info("Booking {} cancelled, seat restored on flight {}", bookingId, booking.getFlightNumber());
    }
}
