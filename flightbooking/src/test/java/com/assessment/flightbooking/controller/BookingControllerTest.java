package com.assessment.flightbooking.controller;

import com.assessment.flightbooking.repository.BookingRepository;
import com.assessment.flightbooking.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full integration tests for {@link BookingController}.
 * <p>
 * Loads the complete Spring application context with a random port and exercises the
 * REST layer end-to-end through {@link MockMvc}. No beans are mocked — all requests
 * flow through the real {@code BookingService}, {@code FlightRepository}, and
 * {@code BookingRepository}. Pre-seeded flights (FL100, FL200, FL300) are restored
 * before every test to guarantee a clean, deterministic starting state.
 * </p>
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class BookingControllerTest {

    private static final String BASE_URL = "/api/v1/bookings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void resetState() {
        // Tests mutate shared in-memory state (bookings created, seats decremented).
        // Resetting before each test prevents order-dependent failures and ensures
        // every test starts from the same 3 pre-seeded flights with full seat counts.
        bookingRepository.clearAll();
        flightRepository.resetAndSeed();
    }

    // =========================================================================
    // POST /api/v1/bookings
    // =========================================================================

    @Test
    @DisplayName("POST /bookings: returns 201 and full BookingResponse body when request is valid")
    void createBooking_validRequest_returns201() throws Exception {
        String requestBody = """
                {
                  "flightNumber": "FL200",
                  "passengerName": "Alice Smith",
                  "passengerEmail": "alice@example.com"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").isNotEmpty())
                .andExpect(jsonPath("$.flightNumber").value("FL200"))
                .andExpect(jsonPath("$.passengerName").value("Alice Smith"))
                .andExpect(jsonPath("$.passengerEmail").value("alice@example.com"))
                .andExpect(jsonPath("$.bookedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings: returns 400 when passengerEmail is not a valid email format")
    void createBooking_invalidEmail_returns400() throws Exception {
        String requestBody = """
                {
                  "flightNumber": "FL200",
                  "passengerName": "Alice Smith",
                  "passengerEmail": "not-an-email"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings: returns 400 when flightNumber is blank")
    void createBooking_blankFlightNumber_returns400() throws Exception {
        String requestBody = """
                {
                  "flightNumber": "",
                  "passengerName": "Alice Smith",
                  "passengerEmail": "alice@example.com"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings: returns 400 when passengerName is blank")
    void createBooking_blankPassengerName_returns400() throws Exception {
        String requestBody = """
                {
                  "flightNumber": "FL200",
                  "passengerName": "   ",
                  "passengerEmail": "alice@example.com"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("POST /bookings: returns 404 when flightNumber does not match any seeded flight")
    void createBooking_unknownFlight_returns404() throws Exception {
        String requestBody = """
                {
                  "flightNumber": "XX999",
                  "passengerName": "Bob Jones",
                  "passengerEmail": "bob@example.com"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Flight not found: XX999"));
    }

    @Test
    @DisplayName("POST /bookings: returns 409 Conflict when FL100 is fully booked (2 seats exhausted)")
    void createBooking_flightFull_returns409() throws Exception {
        // FL100 has exactly 2 seats — book both to exhaust capacity
        String requestBody1 = """
                {
                  "flightNumber": "FL100",
                  "passengerName": "Passenger One",
                  "passengerEmail": "one@example.com"
                }
                """;
        String requestBody2 = """
                {
                  "flightNumber": "FL100",
                  "passengerName": "Passenger Two",
                  "passengerEmail": "two@example.com"
                }
                """;
        String requestBody3 = """
                {
                  "flightNumber": "FL100",
                  "passengerName": "Passenger Three",
                  "passengerEmail": "three@example.com"
                }
                """;

        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody1))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(requestBody2))
                .andExpect(status().isCreated());

        // Third booking must be rejected
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Flight FL100 is fully booked"));
    }

    // =========================================================================
    // DELETE /api/v1/bookings/{bookingId}
    // =========================================================================

    @Test
    @DisplayName("DELETE /bookings/{bookingId}: returns 204 when a valid existing booking is cancelled")
    void cancelBooking_validId_returns204() throws Exception {
        // First create a booking to obtain a real bookingId
        String requestBody = """
                {
                  "flightNumber": "FL200",
                  "passengerName": "Carol White",
                  "passengerEmail": "carol@example.com"
                }
                """;

        String responseJson = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract bookingId from the JSON response without an additional library
        String bookingId = responseJson.replaceAll(".*\"bookingId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(delete(BASE_URL + "/" + bookingId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /bookings/{bookingId}: returns 404 when bookingId does not exist")
    void cancelBooking_nonExistentId_returns404() throws Exception {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(delete(BASE_URL + "/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Booking not found: " + nonExistentId));
    }

    @Test
    @DisplayName("DELETE /bookings/{bookingId}: returns 400 when bookingId is not a valid UUID format")
    void cancelBooking_invalidUUID_returns400() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
