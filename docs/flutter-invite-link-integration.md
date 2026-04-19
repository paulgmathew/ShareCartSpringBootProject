# Flutter Copilot Instructions — Invite By Link Feature

## Context For Copilot

You are working on a Flutter mobile app called ShareCart — a collaborative shopping list app.
The backend is a Spring Boot REST API running at:

- Android emulator: `http://10.0.2.2:8080/api/v1`
- iOS simulator: `http://127.0.0.1:8080/api/v1`

The Flutter app already has the following fully working:

- Register and login (JWT stored in flutter_secure_storage)
- Bearer token attached to all protected requests
- Home screen calling `GET /api/v1/lists/me`
- Create list calling `POST /api/v1/lists` (name only, no ownerId)
- Get list by ID calling `GET /api/v1/lists/{id}`
- Invite user by userId calling `POST /api/v1/lists/{id}/invite`
- Add / update / delete item

The existing service method signatures follow this convention:

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

Your task is to implement the **invite-by-link** feature so a list owner can share a link and recipients can join the list by tapping it.

---

## New Backend Endpoints

Three new endpoints have been added to the backend. Integrate them exactly as described below.

---

### Endpoint 1 — Generate Invite Link

```
POST /api/v1/lists/{listId}/invite-link
Authorization: Bearer <token>
```

Request body: none

Success response — `200 OK`:

```json
{
  "inviteUrl": "https://sharecart.app/invite/abc123def456..."
}
```

Error cases:

- `403` — caller is not the list owner
- `404` — list not found

---

### Endpoint 2 — Accept Invite

```
POST /api/v1/invites/{token}/accept
Authorization: Bearer <token>
```

`{token}` is the raw token at the end of the invite URL (everything after the last `/`).

Request body: none

Success response — `200 OK`:

```json
{
  "listId": "22222222-2222-2222-2222-222222222222",
  "message": "Joined successfully"
}
```

Error cases:

- `400` — invite link has expired
- `403` — missing or invalid JWT
- `404` — token not found or invalid
- `409` — user is already a member of this list

---

### Endpoint 3 — Preview Invite (Public)

```
GET /api/v1/invites/{token}
```

No Authorization header required — this is a public endpoint so the user can preview before logging in.

Success response — `200 OK`:

```json
{
  "listName": "Weekend Groceries",
  "ownerName": "Paul"
}
```

Error cases:

- `404` — token not found or invalid

---

## What To Build In Flutter

### Step 1 — Data Models

Create the following Dart model classes (or records/freezed — match your project convention):

**`InviteLinkResponse`**

```dart
class InviteLinkResponse {
  final String inviteUrl;
}
```

**`AcceptInviteResponse`**

```dart
class AcceptInviteResponse {
  final String listId;
  final String message;
}
```

**`InvitePreviewResponse`**

```dart
class InvitePreviewResponse {
  final String listName;
  final String? ownerName;
}
```

---

### Step 2 — API / Service Methods

Add three methods to the existing shopping list / invite service layer.

```dart
// Generate a shareable invite link for a list the current user owns.
// Requires auth. Returns the full invite URL string.
Future<String> generateInviteLink(String listId);

// Accept an invite using the token extracted from the invite URL.
// Requires auth. Returns the listId the user just joined.
Future<AcceptInviteResponse> acceptInvite(String token);

// Fetch preview info for an invite link — no auth required.
// Use this to show list name/owner before user logs in.
Future<InvitePreviewResponse> getInvitePreview(String token);
```

Implementation notes:

- `generateInviteLink` calls `POST /api/v1/lists/{listId}/invite-link` with the bearer token. Return `response['inviteUrl']` as a `String`.
- `acceptInvite` calls `POST /api/v1/invites/{token}/accept` with the bearer token. The `{token}` is the raw path segment — do NOT send the full URL.
- `getInvitePreview` calls `GET /api/v1/invites/{token}` — no Authorization header needed.

---

### Step 3 — Token Extraction Utility

Add a utility function to extract the raw token from a full invite URL:

```dart
String extractInviteToken(String inviteUrl) {
  return Uri.parse(inviteUrl).pathSegments.last;
}
```

Use this whenever you need to call `acceptInvite` or `getInvitePreview` from a received URL.

---

### Step 4 — Generate Invite Link UI

On the list detail screen (or a "Share" bottom sheet/dialog), add a share button visible only when `memberRole == 'OWNER'`.

Flow:

1. User taps share button
2. Call `generateInviteLink(listId)`
3. Show loading indicator while waiting
4. On success, use `Share.share(inviteUrl)` (from the `share_plus` package) to open the native share sheet
5. On `403` show a snackbar: `"Only the list owner can share this list"`
6. On any other error show a generic snackbar: `"Could not generate invite link. Try again."`

---

### Step 5 — Handle Incoming Invite Link (Deep Link)

The invite URL looks like: `https://sharecart.app/invite/<token>`

Set up deep link handling using `app_links` or `uni_links` (whichever is already in your project):

1. Listen for incoming links on app start and when the app comes to foreground
2. When a link matching `https://sharecart.app/invite/*` is received, extract the token
3. If the user is **not logged in**, store the pending token and redirect to login/register. After successful auth, resume from step 4.
4. If the user **is logged in**, navigate to the Invite Preview Screen (Step 6)

---

### Step 6 — Invite Preview Screen

Create a new screen: `InvitePreviewScreen`

Route parameter: `String token`

On load:

1. Call `getInvitePreview(token)` — no auth required so the screen can render even before auth
2. Show:
   - List name (large heading)
   - Owner name (`"Shared by {ownerName}"`)
   - A primary "Join List" button
   - A secondary "Cancel" / "Go back" link

On "Join List" tapped:

1. If user is not logged in, navigate to login (pass token as a return argument)
2. If user is logged in, call `acceptInvite(token)`
3. Handle responses:
   - `200` — navigate to the list detail screen using the returned `listId`, show snackbar: `"You joined the list!"`
   - `400` — show error: `"This invite link has expired"`
   - `409` — show error: `"You are already a member of this list"` and navigate to the list detail screen
   - `404` — show error: `"Invalid invite link"`
   - `403` — redirect to login

---

### Step 7 — Error Handling Reference

All three endpoints return errors in this standard shape when handled by the backend:

```json
{
  "timestamp": "2026-04-09T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invite link has expired"
}
```

Read the `message` field for display-friendly text. The `status` field maps to:

| Status | Meaning | Flutter action |
|--------|---------|----------------|
| 400 | Expired invite | Show "This invite link has expired" |
| 403 | Not authorized / not owner | Show "You don't have permission" or redirect to login |
| 404 | Token not found | Show "Invalid invite link" |
| 409 | Already a member | Show "Already joined" and open the list |

---

## Checklist For Copilot

Work through these in order:

1. Add `InviteLinkResponse`, `AcceptInviteResponse`, `InvitePreviewResponse` model classes
2. Add `extractInviteToken(String url)` utility function
3. Add `generateInviteLink`, `acceptInvite`, `getInvitePreview` to the API/service layer
4. Add share button to list detail screen, gated on `memberRole == 'OWNER'`
5. Wire `generateInviteLink` to the share button using `share_plus`
6. Set up deep link listener for `https://sharecart.app/invite/*`
7. Create `InvitePreviewScreen` with `getInvitePreview` call and "Join List" button
8. Wire `acceptInvite` to the "Join List" button with full error handling
9. Handle the pending-token-after-login flow for users who tap the link before logging in

---

## Do Not Change

- Existing `inviteUser(String listId, String userId)` method — this is a separate feature (invite by user ID search) that must remain working
- Any existing auth flow, token storage, or interceptor logic
- Existing list detail screen structure — add the share button without restructuring the screen
