package com.assessment.flightbooking.service;

import com.assessment.flightbooking.exception.BookingNotFoundException;
import com.assessment.flightbooking.exception.FlightFullException;
import com.assessment.flightbooking.exception.FlightNotFoundException;
import com.assessment.flightbooking.model.Booking;
import com.assessment.flightbooking.model.Flight;
import com.assessment.flightbooking.repository.BookingRepository;
import com.assessment.flightbooking.repository.FlightRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BookingService}.
 * <p>
 * Verifies the core booking business logic in isolation using Mockito mocks for both
 * repository dependencies. Covers happy-path creation and cancellation, all exception
 * paths ({@link FlightNotFoundException}, {@link FlightFullException},
 * {@link BookingNotFoundException}), and a concurrency scenario that validates
 * the overbooking guard under simultaneous thread access.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    // -------------------------------------------------------------------------
    // createBooking()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("createBooking: successfully creates booking and decrements availableSeats by 1")
    void createBooking_success() {
        Flight flight = buildFlight("FL200", 10);
        when(flightRepository.findByFlightNumber("FL200")).thenReturn(Optional.of(flight));

        Booking result = bookingService.createBooking("FL200", "Alice Smith", "alice@example.com");

        // Seat count must have dropped by one
        assertThat(flight.getAvailableSeats()).isEqualTo(9);

        // Booking fields must be populated correctly
        assertThat(result.getFlightNumber()).isEqualTo("FL200");
        assertThat(result.getPassengerName()).isEqualTo("Alice Smith");
        assertThat(result.getPassengerEmail()).isEqualTo("alice@example.com");
        assertThat(result.getBookingId()).isNotNull();
        assertThat(result.getBookedAt()).isNotNull();

        // Flight must be saved back and booking must be persisted
        verify(flightRepository).save(flight);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking: throws FlightNotFoundException when flight does not exist")
    void createBooking_flightNotFound() {
        when(flightRepository.findByFlightNumber("XX999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking("XX999", "Bob", "bob@example.com"))
                .isInstanceOf(FlightNotFoundException.class)
                .hasMessageContaining("XX999");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking: throws FlightFullException when flight has 0 available seats")
    void createBooking_flightFull() {
        Flight flight = buildFlight("FL100", 0);
        when(flightRepository.findByFlightNumber("FL100")).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking("FL100", "Carol", "carol@example.com"))
                .isInstanceOf(FlightFullException.class)
                .hasMessageContaining("FL100");

        // Seat count must not have changed
        assertThat(flight.getAvailableSeats()).isEqualTo(0);
        verify(bookingRepository, never()).save(any());
    }

    /**
     * Concurrency test: two threads race to book the single remaining seat on a flight.
     * <p>
     * A {@link CountDownLatch} with count 1 is used as a starting pistol so that both
     * threads are constructed and waiting before either is allowed to enter
     * {@code createBooking()}, maximising the chance of a genuine interleaving.
     * Exactly one thread must succeed; the other must throw {@link FlightFullException}.
     * If the {@code synchronized} guard were absent, both threads could read
     * {@code availableSeats == 1}, both pass the check, and both decrement — leaving
     * the seat count at -1 (overbooking).
     * </p>
     */
    @Test
    @DisplayName("createBooking: exactly one thread succeeds and one gets FlightFullException when racing for the last seat")
    void createBooking_concurrency_onlyOneThreadSucceeds() throws InterruptedException {
        Flight flight = buildFlight("FL100", 1);
        // Both threads must receive the SAME Flight instance so they contend on the same monitor
        when(flightRepository.findByFlightNumber("FL100")).thenReturn(Optional.of(flight));

        CountDownLatch startLatch = new CountDownLatch(1); // holds both threads until released
        CountDownLatch doneLatch  = new CountDownLatch(2); // main thread waits for both to finish

        AtomicInteger successCount      = new AtomicInteger(0);
        AtomicInteger flightFullCount   = new AtomicInteger(0);

        Runnable bookingTask = () -> {
            try {
                startLatch.await(); // park until both threads are ready
                bookingService.createBooking("FL100", "Passenger", "p@example.com");
                successCount.incrementAndGet();
            } catch (FlightFullException e) {
                flightFullCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        Thread t1 = new Thread(bookingTask);
        Thread t2 = new Thread(bookingTask);
        t1.start();
        t2.start();

        startLatch.countDown(); // release both threads simultaneously
        doneLatch.await();      // wait for both to finish

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(flightFullCount.get()).isEqualTo(1);
        assertThat(flight.getAvailableSeats()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // cancelBooking()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("cancelBooking: successfully cancels booking and increments availableSeats by 1")
    void cancelBooking_success() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = buildBooking(bookingId, "FL200");
        Flight flight   = buildFlight("FL200", 5);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(flightRepository.findByFlightNumber("FL200")).thenReturn(Optional.of(flight));
        when(bookingRepository.deleteById(bookingId)).thenReturn(true);

        bookingService.cancelBooking(bookingId);

        // Seat must have been restored
        assertThat(flight.getAvailableSeats()).isEqualTo(6);

        verify(flightRepository).save(flight);
        verify(bookingRepository).deleteById(bookingId);
    }

    @Test
    @DisplayName("cancelBooking: throws BookingNotFoundException when bookingId does not exist")
    void cancelBooking_bookingNotFound() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessageContaining(bookingId.toString());

        verify(flightRepository, never()).save(any());
        verify(bookingRepository, never()).deleteById(any());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Flight buildFlight(String flightNumber, int availableSeats) {
        return Flight.builder()
                .flightNumber(flightNumber)
                .origin("London")
                .destination("Paris")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(150)
                .availableSeats(availableSeats)
                .build();
    }

    private Booking buildBooking(UUID bookingId, String flightNumber) {
        return Booking.builder()
                .bookingId(bookingId)
                .flightNumber(flightNumber)
                .passengerName("Test Passenger")
                .passengerEmail("test@example.com")
                .bookedAt(LocalDateTime.now())
                .build();
    }
}
