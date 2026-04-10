# ShareCart API Input/Output Reference

## Purpose

This document lists all current API calls with:

1. Auth requirement
2. Request input
3. Response output
4. Common errors

Base URL:

- Android emulator: http://10.0.2.2:8080/api/v1
- iOS simulator: http://127.0.0.1:8080/api/v1
- Flutter web (same machine): http://localhost:8080/api/v1
- Physical device: http://<your-local-ip>:8080/api/v1

---

## Authentication Rules

Public endpoints:

1. POST /api/v1/auth/register
2. POST /api/v1/auth/login

Protected endpoints:

All other endpoints require header:

Authorization: Bearer <token>

---

## Realtime Channel (WebSocket + STOMP)

WebSocket endpoint:

- ws://<host>/ws

STOMP CONNECT header (required):

- Authorization: Bearer <token>

Topic pattern:

- /topic/lists/{listId}

Subscription rule:

- user must be owner/member of {listId}; otherwise subscription is rejected

Event payload model:

```json
{
  "eventType": "ITEM_ADDED | ITEM_UPDATED | ITEM_DELETED",
  "listId": "uuid",
  "item": {
    "id": "uuid",
    "listId": "uuid",
    "name": "string",
    "quantity": "string or null",
    "isCompleted": "boolean",
    "category": "string or null",
    "createdBy": "uuid or null",
    "createdAt": "ISO local datetime",
    "updatedAt": "ISO local datetime"
  },
  "occurredAt": "ISO instant"
}
```

---

## 1) Register

Endpoint:

POST /api/v1/auth/register

Auth:

No

Request body:

```json
{
  "email": "paul@example.com",
  "password": "password123",
  "name": "Paul"
}
```

Validation:

1. email: required, valid email
2. password: required, minimum 8 chars
3. name: optional

Success:

1. Status: 201 Created
2. Body:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "userId": "11111111-1111-1111-1111-111111111111",
  "email": "paul@example.com",
  "name": "Paul"
}
```

Common errors:

1. 400 Bad Request (validation)
2. 409 Conflict (email already registered)

---

## 2) Login

Endpoint:

POST /api/v1/auth/login

Auth:

No

Request body:

```json
{
  "email": "paul@example.com",
  "password": "password123"
}
```

Validation:

1. email: required, valid email
2. password: required

Success:

1. Status: 200 OK
2. Body:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "userId": "11111111-1111-1111-1111-111111111111",
  "email": "paul@example.com",
  "name": "Paul"
}
```

Common errors:

1. 400 Bad Request (validation)
2. 401 Unauthorized (invalid email or password)

---

## 3) Get My Lists

Endpoint:

GET /api/v1/lists/me

Auth:

Yes

Request body:

None

Success:

1. Status: 200 OK
2. Body: array of MyListResponse

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

Field notes:

1. memberRole is the current user role in that list (OWNER or MEMBER)
2. includes lists user owns and lists user joined

Common errors:

1. 403 Forbidden (missing/invalid/expired token)

---

## 4) Create List

Endpoint:

POST /api/v1/lists

Auth:

Yes

Request body:

```json
{
  "name": "Weekend Groceries"
}
```

Validation:

1. name: required, not blank

Important behavior:

1. ownerId is not accepted from client
2. backend derives owner from JWT user
3. backend auto-adds owner as OWNER member

Success:

1. Status: 201 Created
2. Body: ShoppingListResponse
3. Header: Location: /api/v1/lists/{id}

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

Common errors:

1. 400 Bad Request (validation)
2. 403 Forbidden (missing/invalid/expired token)

---

## 5) Get List By ID

Endpoint:

GET /api/v1/lists/{id}

Auth:

Yes

Path params:

1. id: UUID (list id)

Request body:

None

Success:

1. Status: 200 OK
2. Body: ShoppingListResponse

```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "name": "Weekend Groceries",
  "ownerId": "11111111-1111-1111-1111-111111111111",
  "ownerName": "Paul",
  "items": [
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
  ],
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
  "updatedAt": "2026-03-22T22:10:00"
}
```

Common errors:

1. 403 Forbidden (missing/invalid/expired token)
2. 404 Not Found (list id not found)

---

## 6) Invite User To List

Endpoint:

POST /api/v1/lists/{id}/invite

Auth:

Yes

Path params:

1. id: UUID (list id)

Request body:

```json
{
  "userId": "33333333-3333-3333-3333-333333333333",
  "role": "MEMBER"
}
```

Validation:

1. userId: required
2. role: optional (defaults to MEMBER if null/blank)

Success:

1. Status: 200 OK
2. Body: empty

Common errors:

1. 403 Forbidden (missing/invalid/expired token)
2. 404 Not Found (list/user not found)
3. 409 Conflict (user already member)

---

## 7) Add Item To List

Endpoint:

POST /api/v1/lists/{listId}/items

Auth:

Yes

Path params:

1. listId: UUID

Request body:

```json
{
  "name": "Milk",
  "quantity": "2",
  "category": "Dairy",
  "createdBy": "11111111-1111-1111-1111-111111111111"
}
```

Validation:

1. name: required, not blank
2. quantity: optional
3. category: optional
4. createdBy: optional UUID

Success:

1. Status: 201 Created
2. Body: ItemResponse
3. Header: Location: /api/v1/items/{id}

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

Common errors:

1. 400 Bad Request (validation)
2. 403 Forbidden (missing/invalid/expired token)
3. 404 Not Found (list/user not found)

---

## 8) Update Item

Endpoint:

PUT /api/v1/items/{id}

Auth:

Yes

Path params:

1. id: UUID (item id)

Request body:

```json
{
  "name": "Milk",
  "quantity": "3",
  "isCompleted": true,
  "category": "Dairy"
}
```

Field behavior:

1. all request fields are optional
2. backend updates only provided fields

Success:

1. Status: 200 OK
2. Body: ItemResponse

```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "listId": "22222222-2222-2222-2222-222222222222",
  "name": "Milk",
  "quantity": "3",
  "isCompleted": true,
  "category": "Dairy",
  "createdBy": "11111111-1111-1111-1111-111111111111",
  "createdAt": "2026-03-22T22:05:00",
  "updatedAt": "2026-03-22T22:06:00"
}
```

Common errors:

1. 403 Forbidden (missing/invalid/expired token)
2. 404 Not Found (item id not found)

---

## 9) Delete Item

Endpoint:

DELETE /api/v1/items/{id}

Auth:

Yes

Path params:

1. id: UUID (item id)

Request body:

None

Success:

1. Status: 204 No Content
2. Body: empty

Common errors:

1. 403 Forbidden (missing/invalid/expired token)
2. 404 Not Found (item id not found)

---

## 10) Generate Invite Link

Endpoint:

POST /api/v1/lists/{listId}/invite-link

Auth:

Yes

Path params:

1. listId: UUID

Request body:

None

Important behavior:

1. Only the list OWNER can generate an invite link
2. Link expires after 24 hours
3. Base URL configured via APP_INVITE_BASE_URL env var (default: https://sharecart.app/invite)

Success:

1. Status: 200 OK
2. Body:

```json
{
  "inviteUrl": "https://sharecart.app/invite/abc123def456..."
}
```

Common errors:

1. 403 Forbidden (caller is not the list owner, or missing/invalid token)
2. 404 Not Found (list not found)

---

## 11) Accept Invite Link

Endpoint:

POST /api/v1/invites/{token}/accept

Auth:

Yes

Path params:

1. token: String (invite token from the link)

Request body:

None

Success:

1. Status: 200 OK
2. Body:

```json
{
  "listId": "22222222-2222-2222-2222-222222222222",
  "message": "Joined successfully"
}
```

Common errors:

1. 400 Bad Request (invite link has expired)
2. 403 Forbidden (missing/invalid/expired JWT token)
3. 404 Not Found (invite token not found or invalid)
4. 409 Conflict (user is already a member of this list)

---

## 12) Preview Invite Link

Endpoint:

GET /api/v1/invites/{token}

Auth:

No

Path params:

1. token: String (invite token from the link)

Request body:

None

Important behavior:

1. Public endpoint — no JWT required
2. Use this to show the list name and owner before the user decides to accept

Success:

1. Status: 200 OK
2. Body:

```json
{
  "listName": "Weekend Groceries",
  "ownerName": "Paul"
}
```

Common errors:

1. 404 Not Found (invite token not found or invalid)

---

## Shared Response Models

### AuthResponse

```json
{
  "token": "string",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "string",
  "name": "string or null"
}
```

### MyListResponse

```json
{
  "id": "uuid",
  "name": "string",
  "ownerId": "uuid or null",
  "ownerName": "string or null",
  "memberRole": "string",
  "createdAt": "ISO local datetime",
  "updatedAt": "ISO local datetime"
}
```

### ShoppingListResponse

```json
{
  "id": "uuid",
  "name": "string",
  "ownerId": "uuid or null",
  "ownerName": "string or null",
  "items": ["ItemResponse"],
  "members": ["MemberResponse"],
  "createdAt": "ISO local datetime",
  "updatedAt": "ISO local datetime"
}
```

### ItemResponse

```json
{
  "id": "uuid",
  "listId": "uuid",
  "name": "string",
  "quantity": "string or null",
  "isCompleted": "boolean",
  "category": "string or null",
  "createdBy": "uuid or null",
  "createdAt": "ISO local datetime",
  "updatedAt": "ISO local datetime"
}
```

### MemberResponse

```json
{
  "userId": "uuid",
  "name": "string or null",
  "email": "string",
  "role": "string",
  "joinedAt": "ISO local datetime"
}
```

### GenerateInviteLinkResponse

```json
{
  "inviteUrl": "string"
}
```

### AcceptInviteResponse

```json
{
  "listId": "uuid",
  "message": "string"
}
```

### InvitePreviewResponse

```json
{
  "listName": "string",
  "ownerName": "string or null"
}
```

---

## Standard Error Body (when handled by GlobalExceptionHandler)

```json
{
  "timestamp": "2026-03-22T22:10:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "field": "error message"
  }
}
```

Notes:

1. 400: validation failures include details map; also returned for expired invite links
2. 401: invalid credentials on login
3. 404: resource not found
4. 409: business conflict (duplicate invite, duplicate email, already list member)
5. 403 on protected routes can come directly from Spring Security when token is missing/invalid; also returned when caller lacks permission (e.g. non-owner generating invite link)
