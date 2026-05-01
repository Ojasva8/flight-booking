package com.assessment.flightbooking.repository;

import com.assessment.flightbooking.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for {@link Booking} entities, keyed by booking UUID.
 * <p>
 * Backed by a {@link ConcurrentHashMap} to support safe concurrent access from
 * multiple booking requests without external locking. The {@link UUID} key guarantees
 * globally unique booking identifiers without a sequence generator.
 * </p>
 * <p>
 * No sample data is preloaded here — bookings are created exclusively through
 * the service layer during the application lifecycle.
 * </p>
 */
@Repository
public class BookingRepository {

    private static final Logger log = LoggerFactory.getLogger(BookingRepository.class);

    /**
     * Primary data store. Key = {@code bookingId} ({@link UUID}) assigned at booking creation time.
     */
    private final ConcurrentHashMap<UUID, Booking> store = new ConcurrentHashMap<>();

    public BookingRepository() {
        log.info("BookingRepository in-memory store initialised and ready.");
    }

    /**
     * Persists a new booking record and returns it.
     * If a booking with the same {@code bookingId} already exists (highly unlikely with UUIDs),
     * it is silently overwritten.
     *
     * @param booking the booking to store; must not be {@code null}
     * @return the saved {@link Booking} instance, unchanged
     */
    public Booking save(Booking booking) {
        store.put(booking.getBookingId(), booking);
        return booking;
    }

    /**
     * Looks up a booking by its unique identifier.
     *
     * @param bookingId the UUID assigned to the booking at creation time
     * @return an {@link Optional} containing the booking if found, or empty if not
     */
    public Optional<Booking> findById(UUID bookingId) {
        return Optional.ofNullable(store.get(bookingId));
    }

    /**
     * Returns all bookings associated with a specific flight.
     * <p>
     * Performs a full linear scan of the store — acceptable at in-memory scale but
     * should be replaced with an index if booking volume grows significantly.
     * </p>
     *
     * @param flightNumber the flight number to filter by
     * @return list of bookings for the given flight, possibly empty
     */
    public List<Booking> findByFlightNumber(String flightNumber) {
        return store.values().stream()
                .filter(b -> b.getFlightNumber().equals(flightNumber))
                .collect(Collectors.toList());
    }

    /**
     * Returns a snapshot of all bookings currently in the store.
     * The returned list is a new {@link ArrayList} — modifications to it
     * do not affect the underlying store.
     *
     * @return mutable list of all bookings, possibly empty
     */
    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Checks whether a booking with the given ID exists.
     *
     * @param bookingId the UUID to check
     * @return {@code true} if the booking exists, {@code false} otherwise
     */
    public boolean existsById(UUID bookingId) {
        return store.containsKey(bookingId);
    }

    /**
     * Removes the booking with the given ID from the store.
     *
     * @param bookingId the UUID of the booking to delete
     * @return {@code true} if a booking was removed, {@code false} if no such booking existed
     */
    public boolean deleteById(UUID bookingId) {
        return store.remove(bookingId) != null;
    }

    /**
     * Removes all bookings from the store.
     * <p>
     * Intended for use in integration tests that need to reset state between test cases.
     * Should not be called in production flows.
     * </p>
     */
    public void clearAll() {
        store.clear();
    }
}
