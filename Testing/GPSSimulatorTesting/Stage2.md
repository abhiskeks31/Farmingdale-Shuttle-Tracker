
# GPS Simulation Testing — Stage 2

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

---

# Stage 2 — OSRM Route Generation and Loading

## 2. Implementation

### 2.1 Route Generation

A Python script named `generate_route.py` was created to retrieve road-following route geometry from OSRM.

The script converts OSRM coordinates from `[longitude, latitude]` to `[latitude, longitude]` and saves them in:

`src/main/resources/simulation-routes.json`

The script was updated after the shuttle stop coordinates were corrected in Supabase.

### 2.2 Updated Shuttle Stops

| Stop ID | Stop Name | Latitude | Longitude |
|---|---|---|---|
| 1 | Farmingdale LIRR | 40.73588158 | -73.44175266 |
| 2 | Nold Hall | 40.749538 | -73.4313365 |
| 3 | Aviation Center | 40.73005236 | -73.42264232 |

### 2.3 Generated Route Results

| Route | Waypoints | Distance | Estimated Driving Time |
|---|---|---|---|
| LIRR → Nold Hall (1 → 2) | 85 | 2.74 km | 5.8 minutes |
| Nold Hall → Aviation Center (2 → 3) | 145 | 3.82 km | 5.5 minutes |
| LIRR → Aviation Center (1 → 3) | 151 | 3.83 km | 6.7 minutes |

All three routes were successfully generated and saved in `simulation-routes.json`.

These routes are based on OSRM road-routing data and are not guaranteed to match the college shuttle's actual driving route.

### 2.4 Java Components

The following components were implemented:

| Component | Purpose |
|---|---|
| `SimulationRoute.java` | Represents a route segment and its ordered GPS waypoints |
| `SimulationRouteService.java` | Loads simulation routes from the JSON file |
| `SimulationRouteController.java` | Exposes route retrieval through REST endpoints |
| `generate_route.py` | Generates road-following GPS coordinates using OSRM |
| `simulation-routes.json` | Stores the generated route geometry |

---

## 3. Stage 2 Postman Testing

**Base URL:** `http://localhost:8080`

### Test 2.1 — Retrieve LIRR to Nold Hall Route

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/routes/1/2
```

**Expected Result:**
- HTTP 200 OK.
- `fromStopId` equals `1`.
- `toStopId` equals `2`.
- Response contains 85 GPS waypoints.

**Actual Result:** Route successfully retrieved with the updated Nold Hall coordinates.

**Status:** PASS

### Test 2.2 — Retrieve Nold Hall to Aviation Center Route

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/routes/2/3
```

**Expected Result:**
- HTTP 200 OK.
- `fromStopId` equals `2`.
- `toStopId` equals `3`.
- Response contains 145 GPS waypoints.

**Actual Result:** Route successfully retrieved.

**Status:** PASS

### Test 2.3 — Retrieve Direct LIRR to Aviation Center Route

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/routes/1/3
```

**Expected Result:**
- HTTP 200 OK.
- `fromStopId` equals `1`.
- `toStopId` equals `3`.
- Response contains 151 GPS waypoints.

**Actual Result:** Route successfully retrieved.

**Status:** PASS

### Test 2.4 — Request an Undefined Reverse Route

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/routes/3/1
```

**Expected Result:** HTTP 404 Not Found.

**Actual Result:** HTTP 404 Not Found.

**Status:** PASS

### Test 2.5 — Request a Nonexistent Route

**Method:** GET

**Endpoint:**

```http
GET http://localhost:8080/api/simulation/routes/99/100
```

**Expected Result:** HTTP 404 Not Found.

**Actual Result:** HTTP 404 Not Found.

**Status:** PASS

---

## 4. Stage 2 Test Summary

| Test | Description | Result |
|---|---|---|
| 2.1 | Retrieve LIRR → Nold Hall | PASS |
| 2.2 | Retrieve Nold Hall → Aviation Center | PASS |
| 2.3 | Retrieve LIRR → Aviation Center | PASS |
| 2.4 | Reject undefined reverse route | PASS |
| 2.5 | Reject nonexistent route | PASS |

**Stage 2 Result: 5/5 tests passed.**

