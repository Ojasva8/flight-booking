package com.assessment.flightbooking.repository;

import com.assessment.flightbooking.model.Flight;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for {@link Flight} entities, keyed by flight number.
 * <p>
 * Backed by a {@link ConcurrentHashMap} to provide thread-safe reads and writes
 * without requiring external synchronization. All operations are O(1) on average.
 * Preloads a small set of sample flights on application startup via {@link PostConstruct}.
 * </p>
 * <p>
 * This repository intentionally has no database dependency — it is designed for
 * rapid prototyping and integration testing without infrastructure overhead.
 * </p>
 */
@Repository
public class FlightRepository {

    private static final Logger log = LoggerFactory.getLogger(FlightRepository.class);

    /**
     * Primary data store. Key = {@code flightNumber} (natural key of {@link Flight}).
     * ConcurrentHashMap guarantees atomic per-key operations and safe concurrent iteration.
     */
    private final ConcurrentHashMap<String, Flight> store = new ConcurrentHashMap<>();

    public FlightRepository() {
        log.info("FlightRepository in-memory store initialised and ready.");
    }

    /**
     * Preloads sample flight data on bean initialization.
     * <ul>
     *   <li><b>FL100</b> — London → Paris, intentionally only 2 seats to make it easy to
     *       trigger "no seats available" scenarios in tests.</li>
     *   <li><b>FL200</b> — New York → Boston, comfortable capacity for bulk booking tests.</li>
     *   <li><b>FL300</b> — Dubai → Singapore, long-haul with the largest capacity.</li>
     * </ul>
     */
    @PostConstruct
    public void seedData() {
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        LocalDateTime dayAfterTomorrow = LocalDateTime.now().plusDays(2);

        seedFlight(Flight.builder()
                .flightNumber("FL100")
                .origin("London")
                .destination("Paris")
                .departureTime(tomorrow.withHour(10).withMinute(0).withSecond(0).withNano(0))
                .totalSeats(2)
                .availableSeats(2)
                .build());

        seedFlight(Flight.builder()
                .flightNumber("FL200")
                .origin("New York")
                .destination("Boston")
                .departureTime(tomorrow.withHour(14).withMinute(0).withSecond(0).withNano(0))
                .totalSeats(150)
                .availableSeats(150)
                .build());

        seedFlight(Flight.builder()
                .flightNumber("FL300")
                .origin("Dubai")
                .destination("Singapore")
                .departureTime(dayAfterTomorrow.withHour(22).withMinute(0).withSecond(0).withNano(0))
                .totalSeats(300)
                .availableSeats(300)
                .build());
    }

    /** Saves a flight and emits the required seed INFO log. */
    private void seedFlight(Flight flight) {
        save(flight);
        log.info("Seeded flight: {} ({} -> {})",
                flight.getFlightNumber(), flight.getOrigin(), flight.getDestination());
    }

    /**
     * Persists or replaces a flight record.
     * If a flight with the same {@code flightNumber} already exists, it is overwritten
     * (last-write-wins semantics, consistent with in-memory store behaviour).
     *
     * @param flight the flight to store; must not be {@code null}
     */
    public void save(Flight flight) {
        store.put(flight.getFlightNumber(), flight);
    }

    /**
     * Looks up a flight by its unique flight number.
     *
     * @param flightNumber the natural key to search by
     * @return an {@link Optional} containing the flight if found, or empty if not
     */
    public Optional<Flight> findByFlightNumber(String flightNumber) {
        return Optional.ofNullable(store.get(flightNumber));
    }

    /**
     * Returns a snapshot of all flights currently in the store.
     * The returned list is a new {@link ArrayList} — modifications to it
     * do not affect the underlying store.
     *
     * @return mutable list of all flights, possibly empty
     */
    public List<Flight> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Checks whether a flight with the given number is present in the store.
     *
     * @param flightNumber the natural key to check
     * @return {@code true} if the flight exists, {@code false} otherwise
     */
    public boolean existsByFlightNumber(String flightNumber) {
        return store.containsKey(flightNumber);
    }

    /**
     * Removes all flights from the store.
     * <p>
     * Intended for use in integration tests that need to reset state between test cases.
     * Should not be called in production flows.
     * </p>
     */
    public void clearAll() {
        store.clear();
    }
}
