# ShareCart Backend Changes For Flutter (Recent Updates)

## Purpose

This document lists the recent backend changes and the exact Flutter changes required.
Use this as a migration checklist for your Flutter project.

---

## Summary Of What Changed

1. JWT authentication is now active.
2. All non-auth APIs now require bearer token.
3. New endpoint added for home screen list loading:
   - GET /api/v1/lists/me
4. Create list API changed:
   - POST /api/v1/lists now uses JWT user as owner
   - ownerId must not be sent from Flutter anymore

---

## Endpoint Contract (Current)

Public endpoints:

1. POST /api/v1/auth/register
2. POST /api/v1/auth/login

Protected endpoints (require Authorization header):

1. GET /api/v1/lists/me
2. POST /api/v1/lists
3. GET /api/v1/lists/{id}
4. POST /api/v1/lists/{id}/invite
5. POST /api/v1/lists/{listId}/items
6. PUT /api/v1/items/{id}
7. DELETE /api/v1/items/{id}

---

## Header You Must Send

For all protected endpoints:

Authorization: Bearer <token>

If missing, invalid, or expired, backend returns 403.

---

## New Home Screen Endpoint

## GET /api/v1/lists/me

Use this after login to load all lists visible to current user.

What it returns:

- Lists owned by the user
- Lists where the user is a member

Example response:

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

## Create List Change (Important)

Old request body (no longer valid):

```json
{
  "name": "Weekend Groceries",
  "ownerId": "11111111-1111-1111-1111-111111111111"
}
```

New request body (current):

```json
{
  "name": "Weekend Groceries"
}
```

Backend now gets owner from JWT token automatically.

---

## Auth Response Shape

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

Flutter actions:

1. Store token securely (flutter_secure_storage).
2. Attach token to all protected requests.
3. On 403, redirect to login.

---

## Flutter Code Adjustments Required

1. Add auth flow in API layer:
   - register(...)
   - login(...)
2. Add token persistence and retrieval in a secure storage service.
3. Add an HTTP interceptor (or request wrapper) to inject bearer token.
4. Replace home landing API call with GET /api/v1/lists/me.
5. Update createList method to remove ownerId from request model.
6. Keep existing list detail and item APIs unchanged.

---

## Suggested Service Method Signatures

```dart
Future<AuthResponseModel> register(RegisterRequest request);
Future<AuthResponseModel> login(LoginRequest request);

Future<List<MyListModel>> getMyLists();
Future<ShoppingListModel> createList(String name);
Future<ShoppingListModel> getListById(String listId);
Future<void> inviteUser(String listId, String userId, {String? role});
Future<ItemModel> addItem(String listId, CreateItemRequest request);
Future<ItemModel> updateItem(String itemId, UpdateItemRequest request);
Future<void> deleteItem(String itemId);
```

---

## Migration Checklist For Flutter Team

1. Remove ownerId from create list request payload.
2. Add Authorization header to all protected calls.
3. Change landing page data source to GET /api/v1/lists/me.
4. Ensure login/register responses are mapped and token saved.
5. Handle 401 on login and 403 on protected APIs.
6. Test full flow:
   - register/login
   - load my lists
   - create list
   - open list by id
   - add/update/delete item
   - invite user

---

## Source Of Truth

For full details, see:

- docs/jwt-auth-implementation.md
- docs/flutter-backend-integration.md
