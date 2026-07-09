# ShareCart Spring Boot Backend Guide For Flutter

## Purpose

This document explains the current ShareCart backend contract for Flutter integration.
It includes JWT auth, list retrieval for the logged-in user, and the latest endpoint behavior.

---

## Current Backend Flows

- Register and login with JWT
- Create and manage shopping lists
- Add, update, and delete list items
- Generate and accept invite links
- Preview invite links without login
- Capture, confirm, compare, and view price history
- Discover nearby stores and register stores

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
- `GET /api/v1/invites/{token}` (invite preview only)

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
7. `POST /api/v1/lists/{listId}/invite-link`
8. `GET /api/v1/invites/{token}` (public)
9. `POST /api/v1/invites/{token}/accept`
10. `POST /api/v1/lists/{listId}/items`
11. `PUT /api/v1/items/{id}`
12. `DELETE /api/v1/items/{id}`
13. `POST /api/v1/prices/capture`
14. `POST /api/v1/prices/confirm`
15. `POST /api/v1/prices/compare`
16. `GET /api/v1/prices/history`
17. `GET /api/v1/stores/nearby`
18. `POST /api/v1/stores`

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

## Generate Invite Link

### Endpoint

```text
POST /api/v1/lists/{listId}/invite-link
Authorization: Bearer <token>
```

### Response

```json
{
  "inviteUrl": "https://sharecart.app/invite/abc123token"
}
```

---

## Invite Preview (Public)

### Endpoint

```text
GET /api/v1/invites/{token}
```

### Behavior

This endpoint is public and does not require a bearer token.

### Response

```json
{
  "listName": "Weekend Groceries",
  "ownerName": "Paul"
}
```

---

## Accept Invite

### Endpoint

```text
POST /api/v1/invites/{token}/accept
Authorization: Bearer <token>
```

### Response

```json
{
  "listId": "22222222-2222-2222-2222-222222222222",
  "message": "Invite accepted"
}
```

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

## Price APIs

### Create Price Capture

#### Endpoint

```text
POST /api/v1/prices/capture
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request body

```json
{
  "rawText": "Milk 2L - 3.99",
  "imageUrl": "https://example.com/receipt.jpg",
  "latitude": 9.9312,
  "longitude": 76.2673
}
```

#### Success

- `201 Created`
- Response:

```json
{
  "captureId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
}
```

### Confirm Prices

#### Endpoint

```text
POST /api/v1/prices/confirm
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request body

```json
{
  "captureId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "scanType": "RECEIPT",
  "source": "OCR",
  "capturedAt": "2026-03-22T22:05:00Z",
  "store": {
    "name": "Fresh Mart",
    "address": "MG Road",
    "latitude": 9.9312,
    "longitude": 76.2673
  },
  "items": [
    {
      "itemName": "Milk",
      "price": 3.99,
      "unit": "L"
    }
  ]
}
```

`source` must be one of: `MANUAL`, `OCR`, `API`.

### Compare Price

#### Endpoint

```text
POST /api/v1/prices/compare
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request body

```json
{
  "itemName": "Milk"
}
```

#### Response

```json
{
  "lowestPrice": 3.49,
  "lowestStoreId": "55555555-5555-5555-5555-555555555555",
  "averagePrice": 3.75,
  "totalEntries": 8
}
```

### Get Price History

#### Endpoint

```text
GET /api/v1/prices/history
Authorization: Bearer <token>
```

Optional query:

```text
GET /api/v1/prices/history?itemName=milk
```

Returns the authenticated user's price entries ordered newest first.

---

## Store APIs

### Find Nearby Stores

#### Endpoint

```text
GET /api/v1/stores/nearby?lat=9.9312&lon=76.2673
Authorization: Bearer <token>
```

### Create Store

#### Endpoint

```text
POST /api/v1/stores
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request body

```json
{
  "name": "Fresh Mart",
  "address": "MG Road",
  "latitude": 9.9312,
  "longitude": 76.2673
}
```

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

- `400` validation errors or bad request input
- `401` invalid login credentials
- `403` missing/invalid/expired token on protected endpoints
- `404` resource not found
- `409` business conflict (for example duplicate member invite)
- `500` unexpected server errors

---

## Flutter Integration Checklist

1. Call login/register and save `token` + `userId`.
2. On app landing page, call `GET /lists/me` with bearer token.
3. On create list, send only `name`.
4. Use invite links for sharing: generate via `POST /lists/{listId}/invite-link`, preview via `GET /invites/{token}`, accept via `POST /invites/{token}/accept`.
5. Use price flow when needed: capture -> confirm -> compare/history.
6. After write operations, refresh list data via `GET /lists/{id}` or refresh home via `GET /lists/me`.
7. On `403`, redirect user to login.

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
- POST /api/v1/lists/{listId}/invite-link
- GET /api/v1/invites/{token} (public)
- POST /api/v1/invites/{token}/accept
- POST /api/v1/lists/{listId}/items
- PUT /api/v1/items/{id}
- DELETE /api/v1/items/{id}
- POST /api/v1/prices/capture
- POST /api/v1/prices/confirm
- POST /api/v1/prices/compare
- GET /api/v1/prices/history?itemName=milk
- GET /api/v1/stores/nearby?lat=...&lon=...
- POST /api/v1/stores

On 403, redirect to login.
Store token in flutter_secure_storage.
```
