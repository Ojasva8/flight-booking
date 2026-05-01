# Flight Booking API

## Overview
A REST API for booking flight tickets built with Spring Boot 3 and Java 21. The client is assumed to already know the flight number they wish to book. The application uses in-memory storage only and runs as a single instance. No authentication is required. Overbooking prevention is handled server-side to ensure flight capacities are not exceeded.

## Prerequisites
- Java 21+
- Maven 3.8+

## How to Run
There are two options to run the application:

1. Using the Spring Boot Maven plugin:
```bash
mvn spring-boot:run
```

2. Building the package and running the JAR:
```bash
mvn clean package
java -jar target/flightbooking-0.0.1-SNAPSHOT.jar
```
*(Note: Replace `flightbooking-0.0.1-SNAPSHOT.jar` with the actual name of the generated JAR file if different).*

The default port is `8080`.

## API Endpoints

| Method | Path | Description | Request Body | Success Response |
|--------|------|-------------|--------------|------------------|
| POST   | `/api/v1/bookings` | Create a booking | JSON body | `201 Created` |
| DELETE | `/api/v1/bookings/{bookingId}` | Cancel a booking | None | `204 No Content` |

## Request & Response Structure

### CreateBookingRequest
```json
{
  "flightNumber": "FL200",
  "passengerName": "John Doe",
  "passengerEmail": "john.doe@example.com"
}
```

### BookingResponse
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "flightNumber": "FL200",
  "passengerName": "John Doe",
  "passengerEmail": "john.doe@example.com",
  "bookingTime": "2023-10-27T10:00:00Z"
}
```

### ErrorResponse
```json
{
  "timestamp": "2023-10-27T10:05:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/bookings"
}
```

## Example curl Requests

### 1. Create a booking on FL200
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "FL200",
    "passengerName": "John Doe",
    "passengerEmail": "john.doe@example.com"
  }'
```

### 2. Cancel the booking using the UUID returned from step 1
```bash
curl -X DELETE http://localhost:8080/api/v1/bookings/123e4567-e89b-12d3-a456-426614174000
```
*(Replace the UUID with the actual ID returned from the booking creation).*

### 3. Attempt to overbook FL100 (only 2 seats)
*(Assuming 2 bookings have already been made for FL100)*
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "FL100",
    "passengerName": "Jane Smith",
    "passengerEmail": "jane.smith@example.com"
  }'
```
**Response Body (409 Conflict):**
```json
{
  "timestamp": "2023-10-27T10:10:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Flight FL100 is fully booked",
  "path": "/api/v1/bookings"
}
```

### 4. Book a non-existent flight
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "FL999",
    "passengerName": "Alice Jones",
    "passengerEmail": "alice.jones@example.com"
  }'
```
**Response Body (404 Not Found):**
```json
{
  "timestamp": "2023-10-27T10:15:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Flight FL999 not found",
  "path": "/api/v1/bookings"
}
```

### 5. Send invalid email
```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "FL200",
    "passengerName": "Bob Brown",
    "passengerEmail": "invalid-email"
  }'
```
**Response Body (400 Bad Request):**
```json
{
  "timestamp": "2023-10-27T10:20:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "passengerEmail: Passenger email must be a valid email address",
  "path": "/api/v1/bookings"
}
```

## Pre-loaded Test Flights

| flightNumber | Route | Departure | Total Seats |
|--------------|-------|-----------|-------------|
| FL100 | London → Paris | tomorrow 10:00 | 2 |
| FL200 | New York → Boston | tomorrow 14:00 | 150 |
| FL300 | Dubai → Singapore | day after tomorrow 22:00 | 300 |

*Note that FL100 intentionally has only 2 seats for testing the overbooking guard.*

## Running Tests
```bash
mvn test
```

## What I Would Improve With More Time
- Persistent storage (PostgreSQL + Spring Data JPA)
- Replace synchronized blocks with database-level optimistic locking
- Idempotency key on booking creation to prevent duplicate submissions
- Paginated endpoint to retrieve bookings by flight (currently excluded per spec)
- Structured JSON logging with correlation/trace IDs for observability
- Load and stress testing for the overbooking guard under high concurrency
