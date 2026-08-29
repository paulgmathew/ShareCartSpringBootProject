# ShareCart Spring Boot Backend

ShareCart is a Spring Boot REST API backend for a shared shopping list application.
It provides JWT-based authentication, collaborative shopping lists, member invitations (by user ID and by link), item management, and a price optimization engine for comparing grocery prices across stores.

## Overview

Current capabilities:

- Register a user and send verification email
- Verify email before first login
- Resend verification email
- Log in and receive a JWT token (verified accounts only)
- Load all lists for the logged-in user
- Create shopping lists
- Fetch list details by id
- Invite members to a list
- Generate shareable invite links
- Accept invite links to join a list
- Preview invite links before login
- Add items to a list
- Update items
- Delete items
- Create a price capture (location + raw text / image)
- Confirm and save item prices from a capture
- Compare prices for an item across stores
- Retrieve personal price history with optional item name filter
- Find nearby stores by GPS coordinates
- Create or resolve a store (deduplicates within 200 m)

## Tech Stack

- Java 21
- Spring Boot 4.0.4
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- Spring Security
- Spring WebSocket (STOMP)
- Spring Boot Actuator
- PostgreSQL
- Hibernate 7
- Lombok
- JJWT 0.12.3

## Architecture

The project follows a layered architecture:

- Controller -> REST API layer
- Service -> business logic
- Repository -> data access
- Database -> PostgreSQL

Main modules:

- `auth`
- `shoppinglist`
- `item`
- `invite`
- `price`
- `realtime`
- `user`
- `common.exception`
- `common.security`

## Project Structure

```text
src/main/java/com/sharecart/sharecart/
  auth/
    controller/
    dto/
    service/
  item/
    controller/
    dto/
    model/
    repository/
    service/
  invite/
    controller/
    dto/
    model/
    repository/
    service/
  price/
    controller/
    dto/
    model/
    repository/
    service/
    util/
  realtime/
    config/
    dto/
    security/
    service/
  shoppinglist/
    controller/
    dto/
    model/
    repository/
    service/
  user/
    model/
    repository/
  common/
    exception/
    security/
```

## Local Setup

Make sure PostgreSQL is running locally on port `5432`.

Create the database:

```sql
CREATE DATABASE sharecartdb;
```

Default datasource settings are configured in `src/main/resources/application.properties`.

Local development values:

- Host: `localhost`
- Port: `5432`
- Database: `sharecartdb`
- Username: `postgres`
- Password: `docker1`

JWT settings:

- `app.jwt.secret`
- `app.jwt.expiration-ms=86400000`

## Run Locally

Start the application:

```bash
./mvnw spring-boot:run
```

Build the project:

```bash
./mvnw clean package
```

Application URL:

- `http://localhost:8080`

Base API path:

- `/api/v1`

## Authentication

Public endpoints:

- `POST /api/v1/auth/register`
- `GET /api/v1/auth/verify-email?token={token}`
- `POST /api/v1/auth/resend-verification`
- `POST /api/v1/auth/login`

All other endpoints require this header, except invite preview (`GET /api/v1/invites/{token}`):

```text
Authorization: Bearer <token>
```

Typical flow:

1. Register with email and password
2. Open the verification link received by email
3. Log in after verification and store the returned JWT token
4. Send the token on every protected request
5. Use `GET /api/v1/lists/me` as the landing-page endpoint

## Quick Start API Examples

### Register

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "paul@example.com",
    "password": "password123",
    "name": "Paul"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "paul@example.com",
    "password": "password123"
  }'
```

### Verify Email

```bash
curl "http://localhost:8080/api/v1/auth/verify-email?token=<verificationToken>"
```

### Resend Verification Email

```bash
curl -X POST http://localhost:8080/api/v1/auth/resend-verification \
  -H "Content-Type: application/json" \
  -d '{
    "email": "paul@example.com"
  }'
```

### Load My Lists

```bash
curl http://localhost:8080/api/v1/lists/me \
  -H "Authorization: Bearer <token>"
```

### Create List

```bash
curl -X POST http://localhost:8080/api/v1/lists \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "name": "Weekend Groceries"
  }'
```

### Generate Invite Link

```bash
curl -X POST http://localhost:8080/api/v1/lists/<listId>/invite-link \
  -H "Authorization: Bearer <token>"
```

### Preview Invite Link (Public)

```bash
curl http://localhost:8080/api/v1/invites/<inviteToken>
```

### Accept Invite Link

```bash
curl -X POST http://localhost:8080/api/v1/invites/<inviteToken>/accept \
  -H "Authorization: Bearer <token>"
```

### Create a Price Capture

```bash
curl -X POST http://localhost:8080/api/v1/prices/capture \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "latitude": 37.7749,
    "longitude": -122.4194,
    "rawText": "Whole Milk 3.49"
  }'
```

### Confirm Prices from a Capture

```bash
curl -X POST http://localhost:8080/api/v1/prices/confirm \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "captureId": "<captureId>",
    "scanType": "MANUAL",
    "source": "MANUAL",
    "capturedAt": "2026-07-09T10:00:00Z",
    "store": {
      "name": "Trader Joe'\''s",
      "address": "123 Main St",
      "latitude": 37.7749,
      "longitude": -122.4194
    },
    "items": [
      { "itemName": "Whole Milk", "price": 3.49, "unit": "gallon" }
    ]
  }'
```

### Compare Prices for an Item

```bash
curl -X POST http://localhost:8080/api/v1/prices/compare \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{ "itemName": "Whole Milk" }'
```

### Find Nearby Stores

```bash
curl "http://localhost:8080/api/v1/stores/nearby?lat=37.7749&lon=-122.4194" \
  -H "Authorization: Bearer <token>"
```

## Current API Endpoints

### Auth

- `POST /api/v1/auth/register`
- `GET /api/v1/auth/verify-email?token={token}`
- `POST /api/v1/auth/resend-verification`
- `POST /api/v1/auth/login`

### Shopping Lists

- `GET /api/v1/lists/me`
- `POST /api/v1/lists`
- `GET /api/v1/lists/{id}`
- `POST /api/v1/lists/{id}/invite`

### Invite Links

- `POST /api/v1/lists/{listId}/invite-link`
- `POST /api/v1/invites/{token}/accept`
- `GET /api/v1/invites/{token}`

### Items

- `POST /api/v1/lists/{listId}/items`
- `PUT /api/v1/items/{id}`
- `DELETE /api/v1/items/{id}`

### Prices

- `POST /api/v1/prices/capture`
- `POST /api/v1/prices/confirm`
- `POST /api/v1/prices/compare`
- `GET /api/v1/prices/history`

### Stores

- `GET /api/v1/stores/nearby`
- `POST /api/v1/stores`

## Important API Notes

- `POST /api/v1/lists` derives the owner from the authenticated JWT user
- Clients must not send `ownerId` when creating a list
- `GET /api/v1/lists/me` is the correct home-screen endpoint
- `PUT /api/v1/items/{id}` behaves like a partial update even though it uses PUT
- `POST /api/v1/lists/{id}/invite` directly adds membership in `list_members`; it is not a pending request workflow
- Current realtime events are item-only (`ITEM_ADDED`, `ITEM_UPDATED`, `ITEM_DELETED`)
- `POST /api/v1/prices/confirm` accepts `source` values of `MANUAL`, `OCR`, or `API`
- `POST /api/v1/stores` deduplicates: if a store with the same name exists within 200 metres it returns the existing record
- `GET /api/v1/prices/history` accepts an optional `itemName` query param to filter results; matching is case-insensitive and partial

## Documentation

Detailed docs:

- `docs/jwt-auth-implementation.md`
- `docs/flutter-backend-integration.md`
- `docs/flutter-recent-backend-changes.md`
- `docs/api-input-output-reference.md`
- `docs/invite-link-implementation.md`
- `docs/flutter-invite-link-integration.md`
- `docs/liquibase-project-guide.md`
- `docs/render-deployment-troubleshooting.md`
- `docs/realtime-websocket-sync.md`
- `docs/realtime-phase2-change-log.md`
- `docs/price-optimization-implementation.md`
- `docs/refactor-ai-priceconfirm.md`
- `docs/sharecart-ai-api-overview.md`
- `docs/sharecart-ai-jwt-authentication-guide.md`

## Development Notes

- The project currently uses `spring.jpa.hibernate.ddl-auto=update` for local schema updates
- Replace the JWT secret before any production deployment
- Local datasource credentials in `application-dev.properties` are for development only
- SQL logging is enabled in dev (`spring.jpa.show-sql=true`) and disabled in prod
- Application-level logs use DEBUG level for `com.sharecart` in dev and INFO in prod
- Email verification mail is sent through Mailtrap Email API using `MAILTRAP_API_TOKEN`
