
## 2. Available Seats Endpoint – Backend Testing

**Project:** Farmingdale Shuttle Tracking System  
**Team:** Shuttle Squad  
**Testing Date:** October 16, 2026  
**Status:** PASS  
**Tools:** Postman, Spring Boot, Supabase PostgreSQL

### 2.1 Objective

Verify that the backend correctly calculates the number of available seats for each shuttle, reflects occupancy updates, handles nonexistent shuttle IDs, and does not modify database records when queried.

**Formula:**

```text
Available Seats = Capacity - Current Passengers
```

### 2.2 API Endpoint

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/shuttles/{shuttleId}/available-seats` | Returns capacity, current passengers, and available seats for a shuttle |

**Example Request:**

```http
GET http://localhost:8080/api/shuttles/1/available-seats
```

**Example Response:**

```json
{
  "shuttleId": 1,
  "capacity": 30,
  "currentPassengers": 9,
  "availableSeats": 21
}
```

### 2.3 Test Cases

| Test ID | Test Description | Expected Result | Status |
|---|---|---|---|
| AS-01 | Retrieve available seats for Shuttle 1 | Correct available seat count returned | PASS |
| AS-02 | Retrieve available seats for Shuttle 2 | Correct available seat count returned | PASS |
| AS-03 | Retrieve available seats for Shuttle 3 | Correct available seat count returned | PASS |
| AS-04 | Request available seats for nonexistent Shuttle 999 | HTTP 404 Not Found | PASS |
| AS-05 | Update occupancy and retrieve available seats | Available seats reflects updated passenger count | PASS |
| AS-06 | Repeatedly retrieve available seats | No changes to occupancy or database history | PASS |

**Results: 6/6 Tests Passed**

### 2.4 Occupancy Integration Testing

The available seats endpoint was tested alongside the previously implemented occupancy tracking functionality.

**Test Procedure:**

1. Retrieve the shuttle's current passenger count using `GET /api/shuttles`.
2. Submit an occupancy update using `POST /api/occupancy/shuttle/{shuttleId}`.
3. Retrieve the updated available seats using `GET /api/shuttles/{shuttleId}/available-seats`.
4. Verify that available seats reflects the updated passenger count.

**Example Occupancy Update:**

```http
POST http://localhost:8080/api/occupancy/shuttle/2
Content-Type: application/json
```

**Request Body:**

```json
{
  "boarding": 5,
  "exiting": 0
}
```

**Expected Available Seats Response (assuming zero initial passengers):**

```json
{
  "shuttleId": 2,
  "capacity": 30,
  "currentPassengers": 5,
  "availableSeats": 25
}
```

**Result:** PASS

The endpoint successfully reflects changes made through the occupancy tracking system.

### 2.5 Error Handling and Database Verification

The following conditions were verified:

- **Nonexistent Shuttle:** Requests for invalid shuttle IDs return HTTP 404 Not Found.
- **Read-Only Requests:** GET requests do not modify passenger counts or create occupancy history records.
- **Current Data:** Available seats is calculated using the shuttle's current capacity and passenger count stored in Supabase.
- **Occupancy Integration:** Successful occupancy updates are reflected in subsequent available seats requests.

### 2.6 Test Conclusion

All six available seats endpoint tests passed successfully.

The backend correctly calculates remaining passenger seats, handles nonexistent shuttle IDs, and retrieves updated occupancy information from Supabase PostgreSQL.

The endpoint is fully functional and ready for integration with the frontend.

**Overall Result: PASS**
