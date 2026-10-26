
# GPS Simulation Testing — Stage 3

## Farmingdale Shuttle Tracking System

**Team:** Shuttle Squad  
**Backend:** Java 21, Spring Boot, Spring Data JPA  
**Database:** Supabase PostgreSQL  
**Routing Engine:** OSRM (Open Source Routing Machine)  
**Testing Tools:** Postman, IntelliJ IDEA, Supabase SQL Editor  

---

## 1. Overview

Stages 2 and 3 focused on implementing a GPS simulation system that allows shuttles to move automatically along road-following routes.

The system uses OSRM to generate geographic waypoints between shuttle stops. These coordinates are stored in a local JSON file and loaded by the Spring Boot backend.

The GPS simulator moves shuttles along these routes at a simulated speed, automatically recording updated coordinates in Supabase PostgreSQL.

### Objectives

- Generate realistic road-following routes using OSRM.
- Store route coordinates in a JSON file.
- Retrieve simulation routes through REST endpoints.
- Support trips containing multiple shuttle stops.
- Simulate automatic shuttle movement.
- Store GPS location history in Supabase.
- Provide endpoints to start, stop, and monitor simulations.
- Validate invalid requests and prevent duplicate simulations.

# Stage 3 — Automatic GPS Movement Simulation

## 5. Implementation

Stage 3 introduced automatic shuttle movement along the OSRM-generated routes.

The simulator retrieves a trip's ordered stops, loads the corresponding road segments, and advances the shuttle's position along the waypoints.

### 5.1 Java Components

| Component | Purpose |
|---|---|
| `SimulationState.java` | Tracks active simulation state and waypoint progress |
| `GPSSimulationService.java` | Handles movement calculations and GPS updates |
| `GPSSimulationController.java` | Provides simulation start, stop, and status endpoints |
| `GPSLocationService.java` | Saves GPS coordinates to Supabase |
| `GPSLocationRepository.java` | Retrieves stored GPS locations |

### 5.2 Simulation Behavior

The simulator implements the following functionality:

- Simulations start manually through a REST endpoint.
- Only one simulation can run per shuttle at a time.
- The simulator retrieves ordered stops using `TripService`.
- Road segments are combined for multi-stop trips.
- Shuttle movement is calculated using geographic distance.
- GPS positions are updated approximately every five seconds.
- Each update creates a new record in `gps_location`.
- Simulations stop automatically when the destination is reached.
- Simulations can also be stopped manually.

### 5.3 Simulation Settings

| Setting | Value |
|---|---|
| Default simulated speed | 30 km/h |
| GPS update interval | 5 seconds |
| Position storage | Supabase PostgreSQL |
| Simulation state storage | Backend application memory |
| Route geometry source | Local JSON file |

---

## 6. Stage 3 Postman Testing

**Base URL:** `http://localhost:8080`

The following tests were performed using the updated routes.

### Test 3.1 — Start Trip 8 Simulation

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/start?tripId=8
```

**Expected Result:**
- HTTP 200 OK.
- Shuttle ID is `1`.
- Trip ID is `8`.
- Simulation starts successfully.
- Route contains 85 waypoints.

**Actual Result:** Simulation started successfully using the updated LIRR → Nold Hall route.

**Status:** PASS

### Test 3.2 — Verify GPS Movement for Trip 8

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/gps-locations/shuttle/1/latest
```

**Procedure:**
1. Start the Trip 8 simulation.
2. Retrieve the shuttle's latest GPS coordinates.
3. Wait approximately 10–15 seconds.
4. Retrieve the latest GPS coordinates again.
5. Compare the recorded positions.

**Expected Result:** Latitude and longitude change as the shuttle advances.

**Actual Result:** Shuttle coordinates updated as the simulation progressed.

**Status:** PASS

### Test 3.3 — Start Multi-Stop Trip 4

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/start?tripId=4
```

**Expected Result:**
- HTTP 200 OK.
- Simulation starts successfully.
- Route includes LIRR → Nold Hall → Aviation Center.
- Combined route contains 229 waypoints.

**Actual Result:** Multi-stop simulation started and GPS movement was verified.

**Status:** PASS

### Test 3.4 — Start Direct Aviation Center Trip 32

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/start?tripId=32
```

**Expected Result:**
- HTTP 200 OK.
- Simulation starts successfully.
- Route includes LIRR → Aviation Center.
- Route contains 151 waypoints.

**Actual Result:** Simulation started successfully and GPS movement was verified.

**Status:** PASS

### Test 3.5 — Retrieve Simulation Status

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/shuttle/1/status
```

**Expected Result:**
- HTTP 200 OK.
- Response includes shuttle ID, trip ID, running status, and waypoint progress.

**Actual Result:** Simulation status endpoint returned the current simulation state.

**Status:** PASS

### Test 3.6 — Prevent Duplicate Simulations

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/start?tripId=8
```

**Procedure:** Send the request while shuttle 1 already has an active simulation.

**Expected Result:** HTTP 409 Conflict.

**Actual Result:** Duplicate simulation request was rejected.

**Status:** PASS

### Test 3.7 — Stop an Active Simulation

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/stop
```

**Expected Result:**
- HTTP 200 OK.
- `running` becomes `false`.
- Automatic GPS updates stop.

**Actual Result:** Simulation stopped successfully.

**Status:** PASS

### Test 3.8 — Reject an Incomplete Trip

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/1/start?tripId=1
```

**Expected Result:** HTTP 400 Bad Request because the trip contains fewer than two stops.

**Actual Result:** Invalid trip request was rejected.

**Status:** PASS

### Test 3.9 — Reject a Nonexistent Shuttle

**Method:** POST

**Endpoint:**

```http
POST http://localhost:8080/api/simulation/shuttle/99999/start?tripId=8
```

**Expected Result:** HTTP 404 Not Found.

**Actual Result:** Nonexistent shuttle request was rejected.

**Status:** PASS

---

## 7. Supabase Database Verification

The simulator stores GPS positions in the existing `gps_location` table.

Each location record contains:

- GPS location ID
- Shuttle ID
- Latitude
- Longitude
- Speed
- Heading
- Recording timestamp

The following query can be used to inspect recorded positions:

```sql
SELECT
    gps_location_id,
    shuttle_id,
    latitude,
    longitude,
    speed,
    heading,
    recorded_at
FROM gps_location
WHERE shuttle_id = 1
ORDER BY recorded_at DESC, gps_location_id DESC
LIMIT 20;
```

**Expected Result:** Multiple GPS records with changing coordinates and timestamps.

**Actual Result:** GPS records were successfully generated and stored in Supabase during the movement tests.

**Status:** PASS

---

## 8. Stage 3 Test Summary

| Test | Description | Result |
|---|---|---|
| 3.1 | Start Trip 8 | PASS |
| 3.2 | Verify Trip 8 GPS movement | PASS |
| 3.3 | Start multi-stop Trip 4 | PASS |
| 3.4 | Start direct Trip 32 | PASS |
| 3.5 | Retrieve simulation status | PASS |
| 3.6 | Prevent duplicate simulations | PASS |
| 3.7 | Stop active simulation | PASS |
| 3.8 | Reject incomplete trip | PASS |
| 3.9 | Reject nonexistent shuttle | PASS |
| Database | Verify Supabase GPS persistence | PASS |

**Stage 3 Result: 10/10 checks passed, including database verification.**

---

## 9. Known Limitations and Future Improvements

### 9.1 Route Accuracy

The routes are generated using OSRM and OpenStreetMap road data. They represent realistic driving routes but may differ from the shuttle's actual route.

Visual route verification on the Leaflet frontend is still pending.

### 9.2 Multi-Stop Route Transitions

Multi-stop trips combine multiple OSRM route segments.

Because OSRM can snap stops to different nearby road positions, the transition between segments should be visually verified to ensure there are no noticeable jumps.

### 9.3 Simulation State Persistence

Active simulation states are stored in application memory.

Restarting Spring Boot resets the active simulations, but GPS records already stored in Supabase remain available.

### 9.4 GPS History Retention

The simulator creates a new GPS record approximately every five seconds.

A future cleanup policy may be implemented to remove old simulated GPS records while retaining useful location history.

### 9.5 Administrative Authorization

Simulation start and stop endpoints should be restricted to authenticated administrators before public deployment.

### 9.6 Frontend Integration

The React and Leaflet frontend has not yet been fully integrated with the GPS simulator.

Future integration will allow the frontend to retrieve GPS coordinates and display moving shuttle markers.

---

## 10. Conclusion

Stages 2 and 3 successfully implemented the backend GPS simulation system for the Farmingdale Shuttle Tracking System.

OSRM-generated road geometry is stored locally and retrieved through Spring Boot REST endpoints. The simulator supports multiple trip configurations, automatically advances shuttle positions, and records GPS history in Supabase PostgreSQL.

Postman testing confirmed successful route retrieval, automatic GPS movement, multi-stop trip support, simulation controls, database persistence, and error handling.

The GPS simulation backend is ready for frontend integration, with visual route verification and administrative authorization remaining as future tasks.
