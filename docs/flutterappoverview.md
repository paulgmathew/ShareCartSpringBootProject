# Share Cart Flutter App Overview

## 1. Purpose

Share Cart is a Flutter mobile application for managing collaborative grocery and shopping lists. It allows multiple users to:

- create and manage shopping lists
- add and update items
- invite other users to collaborate
- join shared lists through links or QR codes
- sync list updates in real time
- scan receipts or price tags
- compare prices and find the best store for an item
- store location for price optimization

The frontend connects to a Spring Boot backend over REST APIs and WebSocket, with JWT-based authentication.

---

## 2. High-level architecture

The app follows a layered architecture:

Screens → Providers → Repositories → Services → ApiClient

This means:

- UI screens are in `lib/screens/`
- Providers hold state and orchestration logic in `lib/providers/`
- Repositories coordinate data access and local state in `lib/repositories/`
- Services wrap backend API calls in `lib/services/`
- `ApiClient` centralizes HTTP requests, auth headers, error handling, and endpoint configuration

Main application entry points:

- `lib/main.dart` — app bootstrap, DI wiring, providers, deep-link handling
- `lib/app.dart` — MaterialApp setup and theme

---

## 3. Tech stack

| Area | Stack |
|---|---|
| Framework | Flutter / Dart |
| State management | Provider + ChangeNotifier |
| HTTP client | `http` |
| Secure storage | `flutter_secure_storage` |
| Local preferences | `shared_preferences` |
| Deep linking | `app_links` |
| Sharing | `share_plus` |
| QR generation | `qr_flutter` |
| QR scanning | `mobile_scanner` |
| Real-time sync | STOMP + WebSocket |
| Image capture | `image_picker` |
| Location | `geolocator` |
| Design system | Material 3 |
| Backend | Spring Boot REST API + JWT + WebSocket |
| AI service | ShareCart AI receipt/price-tag extraction |

---

## 4. App structure

```text
lib/
├── main.dart
├── app.dart
├── config/
│   └── api_config.dart
├── models/
│   ├── auth_response_model.dart
│   ├── shopping_list_model.dart
│   ├── shopping_list_summary_model.dart
│   ├── item_model.dart
│   ├── member_model.dart
│   ├── invite_link_response_model.dart
│   ├── accept_invite_response_model.dart
│   ├── invite_preview_model.dart
│   ├── receipt_extraction_model.dart
│   ├── confirm_prices_request_model.dart
│   ├── item_price_model.dart
│   ├── list_realtime_event_model.dart
│   ├── api_error_model.dart
│   └── models.dart
├── services/
│   ├── api_client.dart
│   ├── auth_api_service.dart
│   ├── shopping_list_api_service.dart
│   ├── item_api_service.dart
│   ├── invite_api_service.dart
│   ├── catalog_api_service.dart
│   ├── price_api_service.dart
│   ├── receipt_extraction_api_service.dart
│   ├── realtime_sync_service.dart
│   ├── pending_invite_service.dart
│   └── services.dart
├── repositories/
│   ├── auth_repository.dart
│   ├── auth_session_repository.dart
│   └── shopping_list_repository.dart
├── providers/
│   ├── auth_provider.dart
│   ├── home_provider.dart
│   ├── list_detail_provider.dart
│   ├── price_provider.dart
│   ├── price_history_provider.dart
│   ├── price_optimization_provider.dart
│   └── ...
├── screens/
│   ├── auth/
│   ├── home/
│   ├── invite/
│   ├── list_detail/
│   ├── price_scan/
│   ├── price_history/
│   └── settings/
└── widgets/
```

---

## 5. User flows and screens

### 5.1 Authentication

The auth flow includes:

- register
- login
- JWT token storage in secure storage
- auto-logout on unauthorized responses

Flow:

- user enters email/password
- frontend calls backend auth endpoints
- token is saved securely
- every protected request uses `Authorization: Bearer <token>`
- if 401 or 403 occurs, app navigates to login state

Most relevant files:

- `lib/services/auth_api_service.dart`
- `lib/repositories/auth_repository.dart`
- `lib/repositories/auth_session_repository.dart`
- `lib/providers/auth_provider.dart`
- `lib/screens/auth/auth_gate.dart`

### 5.2 Home screen

The home screen loads all shopping lists that are visible to the current user.

The frontend expects backend list fetching through:

- `GET /api/v1/lists/me`

This returns lists the user owns or is a member of.

From the home screen users can:

- create a new shopping list
- open a list
- scan a QR invite to join a list
- view recent features like price history

### 5.3 List detail screen

The list detail view is the central collaborative screen.

Users can:

- see list metadata
- add items
- edit items
- delete items
- mark items complete/incomplete
- view members
- invite collaborators
- generate invite links
- view live list updates

The list detail provider subscribes to real-time updates from a WebSocket/STOMP connection.

### 5.4 Invite flow

The app supports:

- invite by user ID
- invite by shareable link
- QR code generation
- QR code scanning to join a list
- deep link handling for invite URLs

The app also supports previewing an invite before login and then continuing the join flow after authentication.

Relevant services:

- `lib/services/invite_api_service.dart`
- `lib/services/pending_invite_service.dart`
- `lib/screens/invite/invite_preview_screen.dart`
- `lib/screens/invite/scan_qr_screen.dart`

### 5.5 Real-time sync

The app includes a WebSocket subscription layer for list updates.

The frontend:

- opens a STOMP WebSocket connection
- subscribes to list-specific updates
- listens for incoming list change events
- refreshes or patches local list state
- triggers resync if the connection or subscription fails

Key files:

- `lib/services/realtime_sync_service.dart`
- `lib/providers/list_detail_provider.dart`
- `lib/models/list_realtime_event_model.dart`

### 5.6 Receipt / price-tag scanning

The app supports image-based extraction from:

- grocery receipts
- price tags

The AI service extracts item rows from the image and returns structured info such as:

- item name
- quantity
- unit
- price
- confidence
- scan type

Relevant files:

- `lib/services/receipt_extraction_api_service.dart`
- `lib/screens/price_scan/price_scan_screen.dart`
- `lib/models/receipt_extraction_model.dart`

### 5.7 Price history and price optimization

This is one of the more advanced features in the app.

Users can:

- capture raw text or extracted item pricing
- confirm prices with location data
- view previous price history by item name
- compare price details across stores
- check the best store for a given item
- save their home location for store comparison

Relevant files:

- `lib/services/price_api_service.dart`
- `lib/providers/price_provider.dart`
- `lib/providers/price_history_provider.dart`
- `lib/providers/price_optimization_provider.dart`
- `lib/screens/price_history/price_history_screen.dart`
- `lib/screens/settings/settings_screen.dart`

---

## 6. Backend endpoints currently used by the frontend

The frontend expects these REST endpoints.

### 6.1 Authentication

| Action | HTTP | Endpoint |
|---|---|---|
| Register | POST | `/api/v1/auth/register` |
| Login | POST | `/api/v1/auth/login` |

### 6.2 Lists

| Action | HTTP | Endpoint |
|---|---|---|
| Get my visible lists | GET | `/api/v1/lists/me` |
| Create list | POST | `/api/v1/lists` |
| Get list by ID | GET | `/api/v1/lists/{id}` |
| Invite user to list | POST | `/api/v1/lists/{id}/invite` |

### 6.3 Invite links

| Action | HTTP | Endpoint |
|---|---|---|
| Generate invite link | POST | `/api/v1/lists/{id}/invite-link` |
| Preview invite token | GET | `/api/v1/invites/{token}` |
| Accept invite | POST | `/api/v1/invites/{token}/accept` |

### 6.4 Items

| Action | HTTP | Endpoint |
|---|---|---|
| Add item | POST | `/api/v1/lists/{listId}/items` |
| Update item | PUT | `/api/v1/items/{id}` |
| Delete item | DELETE | `/api/v1/items/{id}` |

### 6.5 Catalog / item lookup

| Action | HTTP | Endpoint |
|---|---|---|
| Search catalog | GET | `/api/v1/catalog/items` |
| Create catalog item | POST | `/api/v1/catalog/items` |

### 6.6 Price / optimization

| Action | HTTP | Endpoint |
|---|---|---|
| Capture price data | POST | `/api/v1/prices/capture` |
| Confirm extracted prices | POST | `/api/v1/prices/confirm` |
| Get price history | GET | `/api/v1/prices/history` |
| Compare prices | POST | `/api/v1/prices/compare` |
| Get nearby stores | GET | `/api/v1/stores/nearby` |
| Get best prices | GET | `/api/v1/prices/best-prices` |
| Get best store | GET | `/api/v1/prices/best-store/{canonicalItemId}` |
| Save user location | PATCH | `/api/v1/users/me/location` |

### 6.7 Receipt extraction

| Action | HTTP | Endpoint |
|---|---|---|
| Extract receipt or price-tag items | POST | `/api/v1/receipt/extract` |

---

## 7. Authentication model

The frontend expects JWT-based auth.

The backend is expected to return an auth response similar to:

```json
{
  "token": "...",
  "tokenType": "Bearer",
  "userId": "uuid",
  "email": "user@example.com",
  "name": "User"
}
```

On the frontend side:

- token is stored securely in `flutter_secure_storage`
- every protected request attaches `Authorization: Bearer <token>`
- if token is invalid or expired, app logs out

This is implemented in:

- `lib/repositories/auth_session_repository.dart`
- `lib/services/api_client.dart`

---

## 8. Data and state patterns

The app uses a strict client-side layered design:

- models define JSON conversion logic
- repositories orchestrate data and persistence
- providers expose state to the UI
- widgets consume providers and render screens

The app is intentionally built around DTO-like model classes such as:

- `ShoppingListModel`
- `ShoppingListSummaryModel`
- `ItemModel`
- `MemberModel`
- `InvitePreviewModel`
- `ReceiptExtractionModel`
- `UserLocationModel`

This makes the app easier to keep aligned with backend JSON contracts.

---

## 9. Local storage and persistence

The app uses two storage strategies.

### 9.1 Secure storage

Used for auth tokens and sensitive session information.

- `flutter_secure_storage`
- key-value storage for JWT/session persistence

### 9.2 Shared preferences

Used for local app state such as known list IDs.

The project notes mention:

- `saved_list_ids` as a persisted preference key

This is useful because there is no backend endpoint to fetch all lists in one shot, so the app keeps a local cache of relevant list IDs for the home screen.

---

## 10. Deep linking and invites

The app supports invite links and deep linking with patterns like:

```text
https://sharecart.app/invite/{token}
```

Behavior:

- if the user is logged in, app opens the invite preview immediately
- if the user is not logged in, the app stores the pending invite token and resumes after login
- after accept, user is navigated to the joined list detail screen

This is a core app feature and a very relevant backend contract because the invite flow depends on public and protected endpoints working together correctly.

---

## 11. Real-time contract

The app expects a STOMP/WebSocket backend connection targeted at:

- `/ws`

It subscribes to list-related events and listens for:

- list update events
- resync requests
- connection lifecycle changes

The frontend expects event payloads to be shaped like a `ListRealtimeEventModel`.

This is important because a backend change to event names, topic names, or payload shape can break collaboration updates immediately.

---

## 12. Backend contract summary for coordination

If the backend team needs a quick summary, this is the most important information:

### required auth

- all protected routes require a bearer token
- invalid/expired tokens should return 401/403
- frontend automatically redirects to login

### list ownership model

- list ownership is determined by the authenticated user
- owners and members both appear on the home screen
- create list requests should not send `ownerId`

### invite flow

- frontend generates invite links for owners
- public preview endpoint returns invite metadata without login
- accept invite requires auth
- join flow supports deep links and QR scan

### realtime sync

- list updates are pushed via WebSocket events
- frontend expects a list event model and resync handling

### price features

- price extraction and confirmation rely on structured AI output
- location is optional but often required for store optimization
- item price history is accessed by item name

---

## 15. What this app is really doing

At a business level, Share Cart is a collaborative grocery app with:

- shared shopping lists
- real-time team coordination
- invite-based collaboration
- AI-assisted price scanning
- smart comparison and store optimization

It is not just a simple list app. It is a collaborative consumer shopping system with pricing intelligence and real-time syncing built into the UX.

---

## 13. Final takeaway

This Flutter app is a full collaborative shopping app with real-time sharing, invite management, AI-based scanning, and price optimization. The backend contract is fairly mature, and the frontend already reflects a modern, production-oriented architecture.

For backend collaboration, the most important thing is not just the basic CRUD flows — it is the coordinated behavior of:

- JWT auth
- list ownership and membership
- invite link flows
- real-time collaboration
- AI extraction results
- price comparison and location data

Those are the areas most likely to affect the app’s runtime behavior.
