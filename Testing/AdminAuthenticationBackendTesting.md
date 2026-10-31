
# Admin Authentication Backend Test Report

**Project:** Farmingdale Shuttle Tracking System  
**Team:** Shuttle Squad  
**Testing Date:** October 30, 2026  
**Component:** Backend — Admin Authentication  
**Testing Type:** Manual Functional and Security Testing  
**Test Status:** Passed (9/9 Tests)

---

## 1. Testing Overview

The purpose of this testing phase was to verify the functionality and security of the administrator authentication system implemented using Spring Security.

The authentication system allows authorized administrators to log in using credentials stored in the Supabase PostgreSQL database. Spring Security verifies the administrator's BCrypt password hash, creates an authenticated session, and restricts access to protected API endpoints.

Testing also verified that public endpoints remain accessible without authentication and that Cross-Site Request Forgery (CSRF) protection prevents unauthorized state-changing requests.

All tests were conducted manually using Windows PowerShell HTTP requests and the Supabase SQL Editor.

---

## 2. Testing Environment

| Component | Technology |
|---|---|
| Backend Framework | Spring Boot 4.1.1 |
| Programming Language | Java 21 |
| Security Framework | Spring Security |
| Database | Supabase PostgreSQL |
| Database Access | Spring Data JPA / Hibernate |
| Password Encryption | BCrypt Password Hashing |
| Authentication Method | Session-Based Authentication |
| Testing Tool | Windows PowerShell |
| Development Environment | IntelliJ IDEA |
| Backend Address | http://localhost:8080 |

---

## 3. Test Cases and Results

| Test ID | Test Description | Expected Result | Actual Result | Status |
|---|---|---|---|---|
| AUTH-01 | Access public routes without authentication | Routes returned successfully | Routes returned from Supabase | PASS |
| AUTH-02 | Anonymous POST request without CSRF token | HTTP 403 Forbidden | HTTP 403 | PASS |
| AUTH-03 | Anonymous POST request with valid CSRF token | HTTP 401 Unauthorized | HTTP 401 | PASS |
| AUTH-04 | Authenticated admin POST without CSRF token | HTTP 403 Forbidden | HTTP 403 | PASS |
| AUTH-05 | Login using valid administrator credentials | Authentication successful | Login successful | PASS |
| AUTH-06 | Retrieve authenticated admin identity | Username and ROLE_ADMIN returned | Correct admin identity returned | PASS |
| AUTH-07 | Verify successful login timestamp | last_login updated in database | Timestamp updated in Supabase | PASS |
| AUTH-08 | Logout authenticated administrator | HTTP 204 No Content | HTTP 204 | PASS |
| AUTH-09 | Access protected endpoint after logout | HTTP 401 Unauthorized | HTTP 401 | PASS |

---

## 4. Detailed Testing Results

### AUTH-01: Public API Access

**Objective:** Verify that student-facing endpoints remain accessible without administrator authentication.

**Endpoint:** GET /api/routes

**Procedure:**
1. Started the Spring Boot application.
2. Opened the routes API without authenticating.
3. Verified that route information was successfully returned.

**Expected Result:** The endpoint returns route information from Supabase without requiring authentication.

**Actual Result:** Route records were successfully retrieved.

**Status:** PASS

### AUTH-02: Anonymous POST Without CSRF Token

**Objective:** Verify that unauthenticated requests cannot perform protected operations without CSRF protection.

**Endpoint:** POST /api/occupancy

**Procedure:**
1. Created an HTTP POST request without authenticating.
2. Omitted the CSRF token.
3. Submitted an empty JSON request body.
4. Inspected the HTTP response.

**Expected Result:** HTTP 403 Forbidden.

**Actual Result:** HTTP 403 Forbidden.

**Status:** PASS

### AUTH-03: Anonymous POST With Valid CSRF Token

**Objective:** Verify that possession of a valid CSRF token does not grant administrative authorization.

**Endpoint:** POST /api/occupancy

**Procedure:**
1. Created a new anonymous HTTP session.
2. Retrieved a valid CSRF token from GET /api/auth/csrf.
3. Submitted a POST request containing the CSRF token without authenticating.
4. Inspected the HTTP response.

**Expected Result:** HTTP 401 Unauthorized.

**Actual Result:** HTTP 401 Unauthorized.

**Status:** PASS

### AUTH-04: Authenticated Admin POST Without CSRF Token

**Objective:** Verify that authenticated administrators must still provide valid CSRF tokens when modifying data.

**Endpoint:** POST /api/occupancy

**Procedure:**
1. Created a session and retrieved a CSRF token.
2. Logged in using valid administrator credentials.
3. Submitted a POST request without including the CSRF token.
4. Inspected the HTTP response.

**Expected Result:** HTTP 403 Forbidden.

**Actual Result:** HTTP 403 Forbidden.

**Status:** PASS

### AUTH-05: Valid Administrator Login

**Objective:** Verify that Spring Security authenticates administrators using credentials stored in Supabase.

**Endpoint:** POST /api/auth/login

**Procedure:**
1. Retrieved a CSRF token.
2. Submitted a valid administrator username and password.
3. Spring Security retrieved the administrator record through AdminRepository.
4. The submitted password was verified against the stored BCrypt hash.
5. Inspected the authentication response.

**Expected Result:** Authentication succeeds.

**Actual Result:**

```json
{
  "authenticated": true
}
```

**Status:** PASS

### AUTH-06: Authenticated Administrator Identity

**Objective:** Verify that Spring Security maintains an authenticated session and correctly assigns administrator permissions.

**Endpoint:** GET /api/auth/me

**Procedure:**
1. Successfully authenticated using the test administrator account.
2. Preserved the session cookie.
3. Requested the authenticated administrator's identity.
4. Verified the username and assigned authority.

**Expected Result:** The endpoint returns the administrator's username and ROLE_ADMIN authority.

**Actual Result:** The authenticated username and ROLE_ADMIN authority were returned.

**Status:** PASS

### AUTH-07: Successful Login Timestamp

**Objective:** Verify that successful administrator logins update the database.

**Database Table:** public.admin

**Procedure:**
1. Successfully authenticated using the test administrator account.
2. Opened the Supabase SQL Editor.
3. Executed a query to retrieve the administrator's last_login value.
4. Confirmed that the timestamp reflected the successful login.

**Expected Result:** The last_login field is updated after authentication.

**Actual Result:** The last_login timestamp was successfully updated.

**Status:** PASS

### AUTH-08: Administrator Logout

**Objective:** Verify that an authenticated administrator can successfully log out.

**Endpoint:** POST /api/auth/logout

**Procedure:**
1. Authenticated using the test administrator account.
2. Retrieved a fresh CSRF token after login.
3. Submitted a POST logout request with the valid CSRF token.
4. Inspected the HTTP response.

**Expected Result:** HTTP 204 No Content.

**Actual Result:** HTTP 204 No Content.

**Status:** PASS

### AUTH-09: Session Invalidation After Logout

**Objective:** Verify that previously authenticated sessions cannot access protected endpoints after logout.

**Endpoint:** GET /api/auth/me

**Procedure:**
1. Successfully authenticated as an administrator.
2. Logged out using the logout endpoint.
3. Attempted to access the protected administrator identity endpoint using the same session.
4. Inspected the HTTP response.

**Expected Result:** HTTP 401 Unauthorized.

**Actual Result:** HTTP 401 Unauthorized.

**Status:** PASS

---

## 5. Testing Findings

The backend authentication system successfully passed all nine manual tests.

The following functionality was verified:

- Public shuttle API endpoints remain accessible without login.
- Administrator credentials are authenticated against the Supabase database.
- BCrypt password verification works with the stored password hash.
- Spring Security maintains authenticated administrator sessions.
- Authenticated administrators receive the ROLE_ADMIN authority.
- Anonymous users cannot access protected administrative operations.
- CSRF protection is enforced on state-changing HTTP requests.
- Successful logins update the administrator's last_login timestamp.
- Logout invalidates the authenticated session.
- Previously authenticated sessions cannot access protected resources after logout.

No unexpected errors were encountered during the completed tests.

---

## 6. Remaining Testing

The following tests will be completed during frontend integration and subsequent development.

| Test ID | Test Description | Status |
|---|---|---|
| AUTH-10 | Login through the frontend Admin Login form | NOT TESTED |
| AUTH-11 | Verify Admin Tools visibility after login | NOT TESTED |
| AUTH-12 | Verify Admin Tools remain hidden from anonymous users | NOT TESTED |
| AUTH-13 | Successfully update shuttle occupancy as an authenticated admin | NOT TESTED |
| AUTH-14 | Verify anonymous users cannot update occupancy through the frontend | NOT TESTED |
| AUTH-15 | Start a shuttle trip using authenticated admin controls | NOT TESTED |
| AUTH-16 | Stop a shuttle trip using authenticated admin controls | NOT TESTED |
| AUTH-17 | Verify logout hides Admin Tools in the frontend | NOT TESTED |
| AUTH-18 | Verify invalid administrator credentials are rejected | NOT TESTED |
| AUTH-19 | Verify inactive administrator accounts cannot authenticate | NOT TESTED |
| AUTH-20 | Verify public driver feedback submission | NOT TESTED |

Automated unit and integration tests will also be considered during the final testing phase.

---

## 7. Conclusion

The backend administrator authentication system has successfully completed its initial manual security testing phase.

Spring Security is integrated with the Supabase PostgreSQL database, allowing administrator authentication through BCrypt password verification and session-based access control.

The completed tests confirm that public API endpoints remain accessible, protected operations require authorization, CSRF protection is enforced, and administrator sessions are invalidated after logout.

All nine completed tests passed.

The next development phase will focus on integrating authentication with the frontend, followed by testing administrative functionality such as occupancy management and shuttle trip controls.

**Overall Result: PASS — Backend Authentication Initial Testing Complete**
