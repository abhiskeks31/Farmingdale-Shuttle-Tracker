# Shuttle Tracking System – Backend Testing

This document outlines the backend tests performed for the **Farmingdale Shuttle Tracking System**. Testing is conducted using Postman to verify REST API functionality, input validation, and database persistence.

## 1. Shuttle Occupancy Tracking

**Testing Date:** October 15, 2026  
**Status:** PASSED  
**Tools:** Postman, Spring Boot, Supabase PostgreSQL  
**Environment:** Local Spring Boot application running on `localhost:8080`, connected to Supabase PostgreSQL.

### 1.1 Objective

Verify that the backend correctly processes passengers boarding and exiting shuttles, enforces capacity restrictions, rejects invalid requests, and persists occupancy changes in the database.

Three shuttle records were available for testing, each with a maximum capacity of 30 passengers.

### 1.2 API Endpoints

| HTTP Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/occupancy/shuttle/{shuttleId}` | Updates passenger occupancy |
| `GET` | `/api/shuttles` | Retrieves shuttle information and current passenger counts |
| `GET` | `/api/occupancy/shuttle/{shuttleId}/latest` | Retrieves the latest occupancy record for a shuttle |
| `GET` | `/api/occupancy` | Retrieves occupancy history records |

### 1.3 Test Cases

| Test ID | Test Description | Expected Result | Status |
|---|---|---|---|
| TC-01 | Add 10 boarding passengers to an empty shuttle | Passenger count increases from 0 to 10 | PASS |
| TC-02 | Remove 4 exiting passengers | Passenger count decreases from 10 to 6 | PASS |
| TC-03 | Add 5 boarding and remove 2 exiting passengers | Passenger count updates from 6 to 9 | PASS |
| TC-04 | Attempt to add 25 passengers when 9 are aboard | Request rejected because capacity would be exceeded | PASS |
| TC-05 | Attempt to remove 15 passengers when only 9 are aboard | Request rejected because exiting passengers exceed occupancy | PASS |
| TC-06 | Submit a negative boarding count | Request rejected due to invalid input | PASS |
| TC-07 | Update a nonexistent shuttle (ID 999) | Request rejected because the shuttle does not exist | PASS |
| TC-08 | Retrieve shuttle information after updates | Updated passenger count is returned correctly | PASS |
| TC-09 | Retrieve the latest occupancy record | Latest occupancy update is returned correctly | PASS |
| TC-10 | Verify database persistence | Updated passenger count is retained in Supabase | PASS |

**Results: 10/10 tests passed.**

### 1.4 Example API Request

**Endpoint:**

`POST http://localhost:8080/api/occupancy/shuttle/1`

**Headers:**

`Content-Type: application/json`

**Request Body:**

```json
{
  "boarding": 10,
  "exiting": 0
}
```

**Expected Result:**

- Shuttle 1's passenger count increases by 10.
- A new occupancy history record is created.
- The updated passenger count is stored in Supabase PostgreSQL.

### 1.5 Validation Testing

The following invalid conditions were tested:

- **Capacity exceeded:** The system rejects updates that would cause passenger occupancy to exceed the shuttle's maximum capacity.
- **Excessive exiting passengers:** The system rejects requests where exiting passengers exceed the current occupancy.
- **Negative passenger counts:** The system rejects negative boarding or exiting values.
- **Invalid shuttle ID:** The system rejects occupancy updates for nonexistent shuttles.

Rejected requests do not modify the shuttle's passenger count.

### 1.6 Database Verification

After successful occupancy updates, the database was checked to confirm that passenger counts were updated correctly.

The following SQL query can be used to verify current occupancy:

```sql
SELECT shuttle_id, capacity, current_passengers
FROM public.shuttle
ORDER BY shuttle_id;
```

The occupancy history can also be retrieved through:

`GET http://localhost:8080/api/occupancy`

### 1.7 Test Conclusion

All 10 occupancy tracking tests passed successfully.

The backend correctly handles passenger boarding and exiting, validates requests against shuttle capacity, prevents invalid occupancy updates, and persists changes in Supabase PostgreSQL.

**Overall Result: PASS**

The occupancy tracking feature is ready for integration with the frontend administrative interface.
