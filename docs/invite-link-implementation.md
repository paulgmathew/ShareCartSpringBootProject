# Invite Link Implementation

## Overview

This document explains everything added to make **sharing a shopping list via an invite link** possible. The feature mirrors how WhatsApp group invites work — a list owner generates a shareable URL, and any authenticated user can join the list by tapping/clicking it.

---

## How It Works (Flow)

1. List owner calls `POST /api/v1/lists/{listId}/invite-link`
2. Backend validates the caller is the OWNER, generates a random token, stores it with a 24-hour expiry, and returns the full invite URL
3. Owner shares the URL (e.g. via WhatsApp, iMessage, etc.)
4. Recipient opens the URL — the Flutter app calls `GET /api/v1/invites/{token}` (no login required) to show a preview of the list name and owner
5. Recipient logs in (if not already) and taps "Join" — the Flutter app calls `POST /api/v1/invites/{token}/accept`
6. Backend validates the token (exists, not expired), checks the user is not already a member, adds them to `list_members`, marks the token as used, and returns the `listId`

---

## New Files Created

### 1. Entity — `invite/model/InviteToken.java`

New JPA entity mapping to a new `invite_tokens` table.

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID (PK) | Auto-generated |
| `list_id` | UUID (FK → shopping_lists) | Not null |
| `token` | VARCHAR(100) | Unique, not null — random 32-char hex string |
| `role` | VARCHAR(50) | Defaults to `MEMBER` |
| `created_by` | UUID (FK → users) | Nullable |
| `expires_at` | TIMESTAMP | Set to now + 24 hours on creation |
| `is_used` | BOOLEAN | Defaults to false; set to true after first accepted use |
| `created_at` | TIMESTAMP | Set automatically via `@PrePersist` |

Hibernate creates this table automatically on startup (`ddl-auto=update`).

---

### 2. Repository — `invite/repository/InviteTokenRepository.java`

Extends `JpaRepository<InviteToken, UUID>`.

Custom methods:
- `findByToken(String token)` — used when accepting or previewing an invite
- `findByShoppingListId(UUID listId)` — available for future use (e.g. listing active invite links for a list)

---

### 3. Service Interface — `invite/service/InviteService.java`

```
generateInviteLink(UUID listId, UUID userId) → GenerateInviteLinkResponse
acceptInvite(String token, UUID userId) → AcceptInviteResponse
getInvitePreview(String token) → InvitePreviewResponse
```

---

### 4. Service Implementation — `invite/service/impl/InviteServiceImpl.java`

**`generateInviteLink`**
- Confirms the list exists (404 if not)
- Confirms the caller is the OWNER via `ShoppingListRepository.existsByIdAndOwnerId` (403 if not)
- Generates a 32-character random hex token (`UUID.randomUUID()` without dashes)
- Saves an `InviteToken` with `expiresAt = now + 24 hours`
- Returns `{ inviteUrl: "<base-url>/<token>" }` — base URL read from `app.invite.base-url` property

**`acceptInvite`**
- Looks up the token (404 if missing)
- Checks `expiresAt` — throws `ExpiredInviteTokenException` → 400 if past expiry
- Checks `list_members` for existing membership → 409 if already a member
- Inserts a new `ListMember` row with the role stored on the token (defaults to `MEMBER`)
- Sets `isUsed = true` on the token
- Returns `{ listId, message: "Joined successfully" }`

**`getInvitePreview`**
- Looks up the token (404 if missing)
- Returns `{ listName, ownerName }` — no auth required

---

### 5. Controller — `invite/controller/InviteController.java`

| Method | Path | Auth | Handler |
|--------|------|------|---------|
| POST | `/api/v1/lists/{listId}/invite-link` | Yes | `generateInviteLink` |
| POST | `/api/v1/invites/{token}/accept` | Yes | `acceptInvite` |
| GET | `/api/v1/invites/{token}` | No | `getInvitePreview` |

The authenticated endpoints extract the current user's UUID from `SecurityContextHolder` (set by `JwtFilter`).

---

### 6. DTOs — `invite/dto/`

| Record | Fields |
|--------|--------|
| `GenerateInviteLinkResponse` | `inviteUrl: String` |
| `AcceptInviteResponse` | `listId: UUID`, `message: String` |
| `InvitePreviewResponse` | `listName: String`, `ownerName: String` |

---

### 7. Exception — `common/exception/ExpiredInviteTokenException.java`

Custom `RuntimeException` thrown when an invite token's `expiresAt` is in the past. Handled by `GlobalExceptionHandler` and mapped to HTTP 400.

---

## Modified Files

### `common/exception/GlobalExceptionHandler.java`

Two new `@ExceptionHandler` methods added:

- `AccessDeniedException` → **403 Forbidden** (covers the "non-owner generating invite" case and any other Spring Security access denial caught by the handler)
- `ExpiredInviteTokenException` → **400 Bad Request**

---

### `common/security/SecurityConfig.java`

The invite preview endpoint is public (users need to see the list name before logging in):

```java
.requestMatchers(HttpMethod.GET, "/api/v1/invites/*").permitAll()
```

All other `/api/v1/invites/**` routes (the `accept` POST) remain protected and require a valid JWT.

---

### `resources/application.properties`

New property added:

```properties
app.invite.base-url=${APP_INVITE_BASE_URL:https://sharecart.app/invite}
```

- In local development this defaults to `https://sharecart.app/invite`
- In production, set the `APP_INVITE_BASE_URL` environment variable to your actual deep link base (e.g. your Flutter app's universal link or a redirect URL)

---

### `docs/api-input-output-reference.md`

Three new endpoint sections added (10, 11, 12) and three new shared response model sections added (`GenerateInviteLinkResponse`, `AcceptInviteResponse`, `InvitePreviewResponse`). Error body notes updated to reflect the new 400 (expired invite) and 403 (permission denied) cases.

---

## Error Reference for Invite Endpoints

| Status | Cause |
|--------|-------|
| 400 | Invite token has expired |
| 403 | Caller is not the list owner (for generate), or JWT missing/invalid (for accept) |
| 404 | Token not found / invalid, or list not found |
| 409 | Caller is already a member of the list |

---

## Package Structure Added

```
invite/
  controller/
    InviteController.java
  dto/
    AcceptInviteResponse.java
    GenerateInviteLinkResponse.java
    InvitePreviewResponse.java
  model/
    InviteToken.java
  repository/
    InviteTokenRepository.java
  service/
    InviteService.java
    impl/
      InviteServiceImpl.java
```
