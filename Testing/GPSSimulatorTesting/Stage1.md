
# GPS Simulation Testing Documentation

## Stage 1: Trip-Based Stop Retrieval

**Project:** Farmingdale Shuttle Tracking System  
**Team:** Shuttle Squad  
**Backend:** Java Spring Boot  
**Database:** PostgreSQL (Supabase)  
**Testing Tool:** Postman  
**Test Status:** PASSED

---

## 1. Overview

The purpose of Stage 1 testing was to verify that the backend can retrieve ordered shuttle stops for a specific trip using the existing `trip`, `schedule`, and `stop` tables.

This functionality is required for the GPS simulator to determine the starting point, intermediate stops, and final destination of a shuttle journey.

The implementation no longer requires the `route_stop` table for trip-based stop retrieval.

### Endpoint Tested

**Method:** GET

**Endpoint:**
```http
http://localhost:8080/api/trips/{tripId}/stops
```

### Expected Functionality

- Retrieve scheduled stops associated with a specified trip.
- Return stops in ascending order based on `stop_order`.
- Retrieve stop names and geographic coordinates from the `stop` table.
- Include scheduled departure times.
- Return HTTP 404 when a trip does not exist.
- Support trips with different numbers of scheduled stops.

---

## 2. Test Cases

### Test 1: Retrieve Stops for a Two-Stop Trip

**Request:**
```http
GET http://localhost:8080/api/trips/8/stops
```

**Description:**

Verifies that the backend retrieves the ordered stops for Trip 8, traveling from Farmingdale LIRR to Nold Hall.

**Expected Results:**

- HTTP 200 OK.
- Two stop records returned.
- Farmingdale LIRR appears first.
- Nold Hall appears second.
- Both records include geographic coordinates and departure times.

**Expected Response:**
```json
[
  {
    "stopId": 1,
    "stopName": "farmingdale_lirr",
    "stopOrder": 1,
    "latitude": 40.73588158,
    "longitude": -73.44175266,
    "departureTime": "09:08:00"
  },
  {
    "stopId": 2,
    "stopName": "nold_hall",
    "stopOrder": 2,
    "latitude": 40.73005236,
    "longitude": -73.42264232,
    "departureTime": "09:18:00"
  }
]
```

**Actual Result:** The endpoint returned the expected two stops in the correct order with their corresponding coordinates and departure times.

**Status:** PASS

---

### Test 2: Retrieve Stops for a Three-Stop Trip

**Request:**
```http
GET http://localhost:8080/api/trips/4/stops
```

**Description:**

Verifies that the backend supports trips containing an intermediate stop and a final destination.

Trip 4 travels from Farmingdale LIRR to the Aviation Center, stopping at Nold Hall.

**Expected Results:**

- HTTP 200 OK.
- Three stop records returned.
- Stops appear in the following order:
  1. Farmingdale LIRR
  2. Nold Hall
  3. Aviation Center
- Each record includes stop ID, stop name, stop order, coordinates, and departure time.

**Actual Result:** The endpoint successfully returned all three scheduled stops in the correct order.

**Status:** PASS

**Note:** The Aviation Center coordinates are being corrected in the database. This test verifies stop retrieval and ordering, not the geographic accuracy of the coordinates.

---

### Test 3: Retrieve Stops for a Nonexistent Trip

**Request:**
```http
GET http://localhost:8080/api/trips/99999/stops
```

**Description:**

Verifies that the backend correctly handles requests for trips that do not exist in the database.

**Expected Results:**

- HTTP 404 Not Found.
- No stop records returned.
- Backend remains operational.

**Actual Result:** The backend returned HTTP 404 Not Found as expected.

**Status:** PASS

---

### Test 4: Retrieve Stops for an Incomplete Trip

**Request:**
```http
GET http://localhost:8080/api/trips/1/stops
```

**Description:**

Verifies that the backend can retrieve available schedule information for a trip containing only one recorded stop.

Trip 1 currently contains a scheduled departure from Farmingdale LIRR but does not have a corresponding Nold Hall stop record.

**Expected Results:**

- HTTP 200 OK.
- One stop record returned.
- Farmingdale LIRR is identified as the first stop.
- The endpoint does not fail because the trip contains fewer than two stops.

**Actual Result:** The endpoint returned the existing scheduled stop without errors.

**Status:** PASS

**Note:** The GPS simulator will require at least two valid stops before beginning movement. This validation will be implemented during a later stage.

---

## 3. Test Results Summary

| Test ID | Test Description | Expected HTTP Status | Result |
|---------|------------------|----------------------|--------|
| GPS-S1-01 | Retrieve two-stop trip | 200 OK | PASS |
| GPS-S1-02 | Retrieve three-stop trip | 200 OK | PASS |
| GPS-S1-03 | Retrieve nonexistent trip | 404 Not Found | PASS |
| GPS-S1-04 | Retrieve incomplete trip | 200 OK | PASS |

**Total Tests:** 4  
**Passed:** 4  
**Failed:** 0

---

## 4. Database Integration Verification

Stage 1 testing confirmed that the backend successfully retrieves and combines data from the following PostgreSQL tables:

| Table | Purpose |
|-------|---------|
| `trip` | Identifies the requested shuttle journey |
| `schedule` | Associates trips with scheduled stops, departure times, and stop order |
| `stop` | Provides stop names and geographic coordinates |

The backend retrieves schedule records using `trip_id`, orders them by `stop_order`, and retrieves the associated stop information using `stop_id`.

This eliminates the need to rely on the `route_stop` table for GPS simulation stop retrieval.

---

## 5. Conclusion

All four Stage 1 test cases passed successfully in Postman.

The backend can now retrieve ordered stops for individual shuttle trips, including stop names, geographic coordinates, and scheduled departure times.

The endpoint also handles nonexistent trips correctly and supports incomplete trip records without causing application errors.

**Stage 1 is complete.**

The next development stage will focus on defining geographic road waypoints between shuttle stops. These waypoints will allow the GPS simulator to generate realistic shuttle movement instead of traveling in straight lines between stop coordinates.
