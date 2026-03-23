# JWT Authentication Implementation — ShareCart

## Overview

This document explains the JWT authentication system added to the ShareCart Spring Boot backend. It covers what was built, why each component exists, how the full auth flow works, and how to use it from a Flutter app.

---

## What Was Built

Two public auth endpoints:

- `POST /api/v1/auth/register` — create an account
- `POST /api/v1/auth/login` — get a JWT token

One new protected list-discovery endpoint:

- `GET /api/v1/lists/me` — fetch all lists for the logged-in user

All non-auth endpoints now require a valid JWT in the `Authorization` header.

---

## Why JWT?

JWT (JSON Web Token) is the industry-standard approach for stateless REST API authentication.

The alternative is session-based auth (cookies), but that requires server-side session storage, which doesn't scale well and doesn't work cleanly with mobile apps.

With JWT:

- the server issues a signed token at login
- the client stores the token locally
- every request includes the token in the `Authorization` header
- the server validates the token without a database lookup
- the server always knows who is making the request

This is a stateless design. No sessions, no cookies, no shared server memory.

---

## Auth Flow (Step By Step)

```text
1. User registers
   POST /api/v1/auth/register
   → password is hashed with BCrypt
   → user is saved to the database
   → a JWT token is returned immediately

2. User logs in
   POST /api/v1/auth/login
   → email is looked up in the database
   → password is compared against the stored hash
   → a JWT token is returned

3. User makes an API request
   GET /api/v1/lists/{id}
   Authorization: Bearer <token>
   → JwtFilter intercepts the request
   → the token is validated and decoded
   → userId is extracted from the token
   → Spring Security knows who is making the request
   → the request proceeds to the controller

4. Token expires
   → the client must call /auth/login again to get a new token
   → default expiry is 24 hours
```

---

## Dependencies Added

**Spring Security** — handles request authentication enforcement:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**JJWT 0.12.3** — the JWT library used for creating and validating tokens:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

JJWT 0.12.3 is the current stable version (not 0.11.x). The API is cleaner and avoids deprecated methods.

---

## Configuration Added (application.properties)

```properties
app.jwt.secret=sharecart-super-secret-jwt-key-replace-this-before-deploying-to-prod!
app.jwt.expiration-ms=86400000
```

- `app.jwt.secret` — the signing key used to create and verify JWTs. Must be at least 32 characters (256 bits) for the HMAC-SHA256 algorithm. Change this before deploying.
- `app.jwt.expiration-ms` — token lifetime in milliseconds. `86400000` = 24 hours.

Also fixed: the datasource URL previously had a trailing semicolon (`...sharecartdb;`) which was incorrect. It is now `...sharecartdb`.

---

## New Files Created

```text
src/main/java/com/sharecart/sharecart/
  auth/
    controller/
      AuthController.java          ← handles /auth/register and /auth/login
    dto/
      RegisterRequest.java         ← request body for registration
      LoginRequest.java            ← request body for login
      AuthResponse.java            ← response containing the JWT token
    service/
      AuthService.java             ← interface
      impl/
        AuthServiceImpl.java       ← register and login business logic
  common/
    exception/
      InvalidCredentialsException.java   ← 401 when credentials are wrong
    security/
      JwtUtil.java                 ← generates and validates JWT tokens
      JwtFilter.java               ← reads JWT on every request
      SecurityConfig.java          ← configures Spring Security rules
```

---

## Component Breakdown

### AuthController

The entry point for auth. Delegates all logic to the service.

- `POST /api/v1/auth/register` → returns `201 Created` with `AuthResponse`
- `POST /api/v1/auth/login` → returns `200 OK` with `AuthResponse`

Both endpoints are open (no JWT required). All other endpoints require a valid JWT.

---

### AuthServiceImpl

**register flow:**

1. Check if the email is already in the database — if so, throw `409 Conflict`
2. Hash the password using BCrypt
3. Save the new user to the database
4. Generate a JWT token
5. Return the token and user info

**login flow:**

1. Look up the user by email
2. If not found, throw `401 Unauthorized` with a generic message (does not reveal whether the email exists)
3. Compare the submitted password against the stored BCrypt hash
4. If wrong, throw `401 Unauthorized`
5. Generate a JWT token
6. Return the token and user info

The generic error message (`"Invalid email or password"`) on both wrong email and wrong password is intentional. It prevents user enumeration attacks, where an attacker could determine if an email is registered by comparing error messages.

---

### JwtUtil

Handles all JWT operations.

**generateToken(userId, email)**:
- Creates a JWT with:
  - `subject` = UUID of the user
  - `email` claim = user's email
  - `issuedAt` = now
  - `expiration` = now + `app.jwt.expiration-ms`
- Signed with HMAC-SHA256 using the configured secret key

**isTokenValid(token)**:
- Parses the token and returns `true` if valid and not expired
- Returns `false` for any parsing error or expired token (no exceptions thrown)

**extractUserId(token)**:
- Parses the token and returns the `subject` field (the user's UUID as a string)

---

### JwtFilter

Runs on every incoming HTTP request before it reaches the controller.

1. Check if the `Authorization` header is present and starts with `Bearer `
2. Extract the raw token string (remove the `"Bearer "` prefix)
3. Validate the token using `JwtUtil.isTokenValid()`
4. If valid, extract the `userId` and set it as the authenticated principal in Spring Security's context
5. Pass the request through to the next filter in the chain

If the token is absent or invalid, the filter does nothing. Spring Security then evaluates whether the endpoint requires authentication and returns `403 Forbidden` if it does.

---

### SecurityConfig

Configures Spring Security rules for the entire application.

```text
CSRF          → disabled (not needed for stateless JWT APIs without browser sessions)
Session       → STATELESS (Spring Security never creates HTTP sessions)
/api/v1/auth/** → public (no token required)
all other URLs → require authenticated JWT
JWT filter    → runs before Spring Security's default auth filter
PasswordEncoder → BCryptPasswordEncoder (12 rounds by default)
```

---

### InvalidCredentialsException

A custom unchecked exception that the `GlobalExceptionHandler` catches and maps to `401 Unauthorized`.

This keeps the error handling consistent with the rest of the project (centralized in `GlobalExceptionHandler`, never in controllers or services directly).

---

## API Reference

### Register

```text
POST /api/v1/auth/register
Content-Type: application/json
```

Request body:

```json
{
  "email": "paul@example.com",
  "password": "securepassword123",
  "name": "Paul"
}
```

Validation rules:
- `email` is required and must be a valid email format
- `password` is required and must be at least 8 characters
- `name` is optional

Success response (`201 Created`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "11111111-1111-1111-1111-111111111111",
  "email": "paul@example.com",
  "name": "Paul"
}
```

Error responses:

- `400 Bad Request` — validation failed (missing email, short password, etc.)
- `409 Conflict` — email is already registered

---

### Login

```text
POST /api/v1/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "paul@example.com",
  "password": "securepassword123"
}
```

Success response (`200 OK`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "11111111-1111-1111-1111-111111111111",
  "email": "paul@example.com",
  "name": "Paul"
}
```

Error responses:

- `400 Bad Request` — validation failed
- `401 Unauthorized` — wrong email or password

---

### Get My Lists

```text
GET /api/v1/lists/me
Authorization: Bearer <token>
```

Returns all lists accessible to the current authenticated user:

- lists owned by the user
- lists where the user is a member

Success response (`200 OK`):

```json
[
  {
    "id": "22222222-2222-2222-2222-222222222222",
    "name": "Weekend Groceries",
    "ownerId": "11111111-1111-1111-1111-111111111111",
    "ownerName": "Paul",
    "memberRole": "OWNER",
    "createdAt": "2026-03-22T22:00:00",
    "updatedAt": "2026-03-22T22:10:00"
  }
]
```

---

### Create List (Updated)

```text
POST /api/v1/lists
Authorization: Bearer <token>
Content-Type: application/json
```

Request body:

```json
{
  "name": "Weekend Groceries"
}
```

Important behavior:

- `ownerId` is no longer accepted from client input
- owner is derived from JWT principal on the backend
- owner is automatically added as an `OWNER` member

Success response (`201 Created`):

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "name": "Weekend Groceries",
  "ownerId": "11111111-1111-1111-1111-111111111111",
  "ownerName": "Paul",
  "items": [],
  "members": [
    {
      "userId": "11111111-1111-1111-1111-111111111111",
      "name": "Paul",
      "email": "paul@example.com",
      "role": "OWNER",
      "joinedAt": "2026-03-22T22:00:00"
    }
  ],
  "createdAt": "2026-03-22T22:00:00",
  "updatedAt": "2026-03-22T22:00:00"
}
```

---

### Calling Protected Endpoints

After login, include the token in every request:

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Example:

```text
GET /api/v1/lists/22222222-2222-2222-2222-222222222222
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

If the token is missing or invalid:

- `403 Forbidden` → no token provided or token rejected

---

## What Is Inside The JWT Token

The JWT payload (claims) contains:

```json
{
  "sub": "11111111-1111-1111-1111-111111111111",
  "email": "paul@example.com",
  "iat": 1711058400,
  "exp": 1711144800
}
```

- `sub` — the user's UUID (used as the principal in Spring Security)
- `email` — the user's email
- `iat` — issued at (Unix timestamp)
- `exp` — expiry (Unix timestamp)

The token is signed but **not encrypted**. Do not store sensitive information in JWT payload other than identifiers.

---

## Getting The Current User In Any Controller

Since the `JwtFilter` sets the authenticated principal, any controller can access the current user's ID like this:

```java
import org.springframework.security.core.context.SecurityContextHolder;

// Extract userId from security context (set by JwtFilter)
String userId = (String) SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();
```

The `userId` is the UUID string of the authenticated user. You can use it to look up the user from the `UserRepository`, enforce ownership rules, or scope queries.

Example usage in a controller:

```java
@GetMapping("/me")
public List<MyListResponse> getMyLists() {
    String userId = (String) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

  return shoppingListService.getMyLists(UUID.fromString(userId));
}
```

---

## Security Considerations

### Password Hashing

Passwords are hashed using **BCrypt** before being stored. BCrypt is a slow hashing function specifically designed for password storage — it resists brute-force attacks. Plain-text passwords are never stored or logged.

### JWT Secret Key

The secret key in `application.properties` is for local development only. Before deploying:
- Replace it with a long, randomly generated value (at least 64 characters)
- Load it from an environment variable or secrets manager (e.g. AWS Secrets Manager, Vault, Kubernetes secrets)
- Never commit a production secret to source control

Example using environment variable:

```properties
app.jwt.secret=${JWT_SECRET}
```

### Token Expiry

Tokens expire after 24 hours (`86400000` ms). When a token expires, the client must log in again to get a new token.

For a more advanced setup later, implement refresh tokens to allow seamless token renewal without full re-authentication.

### User Enumeration Protection

The login endpoint returns the same `"Invalid email or password"` message whether the email does not exist or the password is wrong. This prevents attackers from discovering which emails are registered.

### No Token Blacklisting

Issued tokens remain valid until expiry, even after logout. For this MVP this is acceptable. A production system might maintain a token blacklist (Redis) for immediate token invalidation on logout.

---

## Flutter Integration

### 1. Register a new user

```text
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "paul@example.com",
  "password": "password123",
  "name": "Paul"
}
```

Store the returned `token` and `userId` in secure local storage (e.g. `flutter_secure_storage`).

### 2. Log in

```text
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "paul@example.com",
  "password": "password123"
}
```

Store the returned `token` and `userId`.

### 3. Call protected endpoints

Add the token to the `Authorization` header on every request:

```dart
final response = await dio.get(
  '/api/v1/lists/me',
  options: Options(headers: {
    'Authorization': 'Bearer $token',
  }),
);
```

Use `GET /api/v1/lists/me` for the home/landing page after login.

When creating a list, send only `name` (do not send `ownerId`):

```dart
await dio.post(
  '/api/v1/lists',
  data: {'name': 'Weekend Groceries'},
  options: Options(headers: {
    'Authorization': 'Bearer $token',
  }),
);
```

### 4. Handle token expiry

If a request returns `403 Forbidden`, the token has expired. Redirect the user to the login screen.

### 5. Recommended Flutter storage

- Store the JWT in `flutter_secure_storage` (not `SharedPreferences`)
- `SharedPreferences` is not encrypted and should not hold tokens
- Clear the stored token on logout

### 6. Updated Copilot instructions for Flutter (add to your Flutter project)

```md
Backend auth is now JWT-based.

Register: POST /api/v1/auth/register
Login: POST /api/v1/auth/login

Both endpoints return:
{
  "token": "...",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "...",
  "name": "..."
}

Every other endpoint requires:
Authorization: Bearer <token>

Home screen endpoint:
GET /api/v1/lists/me

Create list request body:
{
  "name": "Weekend Groceries"
}

Store token in flutter_secure_storage.
Add token to all HTTP request headers.
On 403 response, redirect to login screen.
```

---

## Error Response Reference

All auth errors follow the same format as the rest of the API.

| Scenario | Status | Message |
|---|---|---|
| Missing or invalid email format | 400 | Validation failed |
| Password too short | 400 | Validation failed |
| Email already registered | 409 | Email is already registered: ... |
| Wrong email or password | 401 | Invalid email or password |
| No token on protected endpoint | 403 | (empty body from Spring Security) |
| Expired or malformed token | 403 | (empty body from Spring Security) |

---

## Module Structure Added

```text
auth/
  controller/     ← AuthController
  dto/            ← RegisterRequest, LoginRequest, AuthResponse
  service/        ← AuthService interface
  service/impl/   ← AuthServiceImpl

common/
  exception/
    InvalidCredentialsException    ← 401 auth errors
  security/
    JwtUtil         ← JWT generation and validation
    JwtFilter       ← per-request interceptor
    SecurityConfig  ← Spring Security rules
```

---

## Future Improvements (Not Implemented Yet)

- **Refresh tokens** — allow clients to get a new access token without re-logging in
- **Logout endpoint** — maintain a server-side token blacklist for immediate invalidation
- **Role-based access control** — restrict endpoints by user role (ADMIN, USER)
- **Rate limiting** — protect login and register from brute-force attempts
- **HTTPS enforcement** — ensure tokens are never transmitted over plain HTTP
- **Token rotation** — issue a new token on each request close to expiry
