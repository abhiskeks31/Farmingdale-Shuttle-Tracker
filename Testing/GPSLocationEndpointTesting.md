
## 3. GPS Location Tracking – Backend Testing

**Project:** Farmingdale Shuttle Tracking System  
**Team:** Shuttle Squad  
**Testing Date:** October 17, 2026  
**Status:** PASS  
**Tools:** Postman, Spring Boot, Supabase PostgreSQL

### 3.1 Objective

Verify that the backend correctly accepts GPS location updates, validates geographic coordinates, retrieves the latest shuttle location, maintains GPS location history, and persists records in Supabase PostgreSQL.

This feature provides the foundation for simulated GPS tracking, allowing shuttle locations to be updated and retrieved without physical GPS hardware.

### 3.2 API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/gps-locations` | Retrieves all stored GPS location records |
| GET | `/api/gps-locations/shuttle/{shuttleId}/latest` | Retrieves the latest GPS location for a specific shuttle |
| POST | `/api/gps-locations/shuttle/{shuttleId}` | Creates a new GPS location record for a shuttle |

### 3.3 Test Cases

| Test ID | Test Description | Expected Result | Status |
|---|---|---|---|
| GPS-01 | Submit valid GPS coordinates for Shuttle 1 | HTTP 201 Created; GPS record saved | PASS |
| GPS-02 | Retrieve latest GPS location for Shuttle 1 | HTTP 200 OK; returns saved coordinates | PASS |
| GPS-03 | Submit another GPS location for Shuttle 1 | New record created; latest location updated | PASS |
| GPS-04 | Submit invalid latitude (100) | HTTP 400 Bad Request | PASS |
| GPS-05 | Submit invalid longitude (-200) | HTTP 400 Bad Request | PASS |
| GPS-06 | Submit negative speed | HTTP 400 Bad Request | PASS |
| GPS-07 | Submit GPS coordinates for nonexistent Shuttle 999 | HTTP 404 Not Found | PASS |
| GPS-08 | Retrieve all GPS location records | Returns stored GPS location history | PASS |
| GPS-09 | Verify GPS records in Supabase PostgreSQL | New records and timestamps are stored correctly | PASS |

**Results: 9/9 Tests Passed**

### 3.4 GPS Location Submission Test

The POST endpoint was tested using Postman to verify that valid GPS coordinates could be submitted and saved.

**Endpoint:**

```http
POST http://localhost:8080/api/gps-locations/shuttle/1
```

**Headers:**

```text
Content-Type: application/json
```

**Example Request Body:**

```json
{
  "latitude": 40.7527,
  "longitude": -73.4288,
  "speed": 15.5,
  "heading": 90
}
```

**Expected Result:**

- HTTP 201 Created.
- A new GPS location record is generated.
- The record is associated with Shuttle 1.
- Latitude, longitude, speed, and heading are stored.
- The backend automatically generates the `recordedAt` timestamp.
- The new record is persisted in Supabase PostgreSQL.

**Result:** PASS

### 3.5 Latest GPS Location Retrieval

The latest-location endpoint was tested to confirm that the backend returns the most recent GPS record for a shuttle.

**Endpoint:**

```http
GET http://localhost:8080/api/gps-locations/shuttle/1/latest
```

**Expected Result:**

- HTTP 200 OK.
- The response contains the latest recorded coordinates.
- The correct shuttle ID is returned.
- The timestamp corresponds to the most recent GPS update.

A second GPS location was submitted to verify that the endpoint returned the newer record rather than the previous location.

**Result:** PASS

### 3.6 GPS Location History Testing

The system was tested to verify that submitting a new GPS location does not overwrite previous location records.

**Endpoint:**

```http
GET http://localhost:8080/api/gps-locations
```

**Expected Result:**

- Previously submitted GPS locations remain stored.
- Newly submitted GPS locations appear as additional records.
- Each record has its own generated GPS location ID and timestamp.

**Result:** PASS

### 3.7 Input Validation Testing

The following invalid requests were tested to verify that the backend rejects incorrect GPS data.

| Validation Test | Invalid Input | Expected Result | Status |
|---|---|---|---|
| Invalid latitude | `"latitude": 100` | HTTP 400 Bad Request | PASS |
| Invalid longitude | `"longitude": -200` | HTTP 400 Bad Request | PASS |
| Negative speed | `"speed": -5` | HTTP 400 Bad Request | PASS |
| Nonexistent shuttle | Shuttle ID `999` | HTTP 404 Not Found | PASS |

The backend successfully rejected invalid GPS submissions without creating valid location updates.

### 3.8 Database Persistence Verification

GPS records were verified in Supabase PostgreSQL after successful POST requests.

The following SQL query can be used to retrieve GPS location records:

```sql
SELECT
    gps_location_id,
    shuttle_id,
    latitude,
    longitude,
    speed,
    heading,
    recorded_at
FROM public.gps_location
ORDER BY recorded_at DESC, gps_location_id DESC;
```

**Expected Result:**

- Submitted GPS coordinates are stored correctly.
- Each successful POST creates a new record.
- Records contain the associated shuttle ID.
- Timestamps are automatically generated.
- Previous GPS records remain available.

**Result:** PASS

### 3.9 Test Conclusion

All nine GPS location tracking tests passed successfully.

The backend correctly accepts and validates GPS coordinates, saves new location records, retrieves the latest shuttle position, and maintains GPS location history in Supabase PostgreSQL.

This functionality provides the foundation for the next development phase: automatically simulating shuttle movement along predefined routes.

**Overall Result: PASS**

**Next Step:** Implement automated round-trip GPS simulation and integrate shuttle movement with the frontend Leaflet map.
