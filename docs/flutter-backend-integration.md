# ShareCart Spring Boot Backend Guide For Flutter

## Purpose

This document explains the current ShareCart backend contract for Flutter integration.
It includes JWT auth, list retrieval for the logged-in user, and the latest endpoint behavior.

---

## Current Backend Flows

- Register and login with JWT
- Create a shopping list (owner is derived from JWT)
- Fetch all lists for the logged-in user
- Fetch a shopping list by ID
- Invite a user to a shopping list
- Add an item to a shopping list
- Update an item
- Delete an item

---

## Base URL

- Android emulator: `http://10.0.2.2:8080/api/v1`
- iOS simulator: `http://127.0.0.1:8080/api/v1`
- Flutter web (same machine): `http://localhost:8080/api/v1`
- Physical device: `http://<your-local-ip>:8080/api/v1`

---

## Authentication

### Public endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Protected endpoints

All other endpoints require:

```text
Authorization: Bearer <token>
```

### Auth response shape

Both register and login return:

```json
{
  "token": "...",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "user@example.com",
  "name": "User"
}
```

Store `token` securely (for example `flutter_secure_storage`) and attach it to every protected call.

---

## Endpoint List

1. `POST /api/v1/auth/register`
2. `POST /api/v1/auth/login`
3. `GET /api/v1/lists/me`
4. `POST /api/v1/lists`
5. `GET /api/v1/lists/{id}`
6. `POST /api/v1/lists/{id}/invite`
7. `POST /api/v1/lists/{listId}/items`
8. `PUT /api/v1/items/{id}`
9. `DELETE /api/v1/items/{id}`

---

## New: Get Lists For Logged-In User

### Endpoint

```text
GET /api/v1/lists/me
Authorization: Bearer <token>
```

### Behavior

Returns all lists accessible to the authenticated user:
- lists the user owns
- lists where the user is a member

### Response

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

### Flutter usage

Use this endpoint for the landing/home screen after login.

---

## Updated: Create Shopping List

### Endpoint

```text
POST /api/v1/lists
Authorization: Bearer <token>
Content-Type: application/json
```

### Request body

```json
{
  "name": "Weekend Groceries"
}
```

### Important change

`ownerId` is no longer accepted from client input.
The backend automatically sets the owner from the JWT user.

### Success

- `201 Created`
- Response body: `ShoppingListResponse`
- Includes `Location: /api/v1/lists/{id}` header

---

## Get Shopping List By ID

### Endpoint

```text
GET /api/v1/lists/{id}
Authorization: Bearer <token>
```

Returns full list details including items and members.

---

## Invite User To List

### Endpoint

```text
POST /api/v1/lists/{id}/invite
Authorization: Bearer <token>
Content-Type: application/json
```

### Request body

```json
{
  "userId": "33333333-3333-3333-3333-333333333333",
  "role": "MEMBER"
}
```

- `role` optional; defaults to `MEMBER`
- duplicate invite returns `409 Conflict`

---

## Add Item

### Endpoint

```text
POST /api/v1/lists/{listId}/items
Authorization: Bearer <token>
Content-Type: application/json
```

### Request body

```json
{
  "name": "Milk",
  "quantity": "2",
  "category": "Dairy",
  "createdBy": "11111111-1111-1111-1111-111111111111"
}
```

- `name` required
- `quantity`, `category`, `createdBy` optional

---

## Update Item

### Endpoint

```text
PUT /api/v1/items/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

All fields are optional; backend updates only provided fields.

---

## Delete Item

### Endpoint

```text
DELETE /api/v1/items/{id}
Authorization: Bearer <token>
```

Success: `204 No Content`.

---

## Core Response Models

### ShoppingListResponse

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "name": "Weekend Groceries",
  "ownerId": "11111111-1111-1111-1111-111111111111",
  "ownerName": "Paul",
  "items": [],
  "members": [],
  "createdAt": "2026-03-22T22:00:00",
  "updatedAt": "2026-03-22T22:00:00"
}
```

### ItemResponse

```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "listId": "22222222-2222-2222-2222-222222222222",
  "name": "Milk",
  "quantity": "2",
  "isCompleted": false,
  "category": "Dairy",
  "createdBy": "11111111-1111-1111-1111-111111111111",
  "createdAt": "2026-03-22T22:05:00",
  "updatedAt": "2026-03-22T22:05:00"
}
```

### MyListResponse

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "name": "Weekend Groceries",
  "ownerId": "11111111-1111-1111-1111-111111111111",
  "ownerName": "Paul",
  "memberRole": "OWNER",
  "createdAt": "2026-03-22T22:00:00",
  "updatedAt": "2026-03-22T22:10:00"
}
```

---

## Error Handling

Common statuses:

- `400` validation errors
- `401` invalid login credentials
- `403` missing/invalid/expired token on protected endpoints
- `404` resource not found
- `409` business conflict (for example duplicate member invite)

---

## Flutter Integration Checklist

1. Call login/register and save `token` + `userId`.
2. On app landing page, call `GET /lists/me` with bearer token.
3. On create list, send only `name`.
4. After write operations, refresh list data via `GET /lists/{id}` or refresh home via `GET /lists/me`.
5. On `403`, redirect user to login.

---

## Paste-Ready Copilot Context For Flutter Project

```md
Backend auth is JWT-based.

Public endpoints:
- POST /api/v1/auth/register
- POST /api/v1/auth/login

Both return:
{
  "token": "...",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "...",
  "name": "..."
}

All other endpoints require:
Authorization: Bearer <token>

Home screen:
- GET /api/v1/lists/me

Create list:
- POST /api/v1/lists
- Request body: { "name": "Weekend Groceries" }
- Do not send ownerId

Other endpoints:
- GET /api/v1/lists/{id}
- POST /api/v1/lists/{id}/invite
- POST /api/v1/lists/{listId}/items
- PUT /api/v1/items/{id}
- DELETE /api/v1/items/{id}

On 403, redirect to login.
Store token in flutter_secure_storage.
```
