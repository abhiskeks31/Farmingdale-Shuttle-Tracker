# Admin Authentication API Documentation

## Login

Our Spring Security login endpoint (`POST /api/auth/login`) expects URL-encoded form fields, not JSON.

The request needs `Content-Type: application/x-www-form-urlencoded` with `username` and `password` as the form fields.

Before logging in, fetch `GET /api/auth/csrf`, which returns the CSRF token and its header name. Send that token in the login request using `[csrf.headerName]: csrf.token`.

We also use `credentials: 'include'` so the browser maintains the authenticated session.

## Session Verification

After successful login, `GET /api/auth/me` can be called to verify the session and retrieve the logged-in admin's username and permissions.

## Logout

Logout uses `POST /api/auth/logout` and requires a valid CSRF token. A successful logout returns HTTP 204 (No Content).

Both login and logout require CSRF tokens, while `GET /api/auth/me` only requires the session cookie.

Authentication is handled internally by Spring Security, so there's no need to implement a separate backend login endpoint.

## Additional Info

- **Successful login:** HTTP 200
- **Invalid login credentials:** HTTP 401
- **Unauthenticated access to protected endpoints:** HTTP 401
- **Missing or invalid CSRF token on protected POST requests:** HTTP 403

All of this has already been implemented and tested in our current authentication code.
