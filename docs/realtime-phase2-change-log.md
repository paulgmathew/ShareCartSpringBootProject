# Realtime Backend Changes (Phase 1 + Phase 2)

## Purpose

This document lists all backend realtime changes implemented in ShareCart and explains why WebSocket + STOMP are used.

## Why WebSocket Is Used

REST is request/response. It is great for initial load, but not ideal when multiple users edit the same list because clients must keep polling.

WebSocket keeps one persistent connection open so server can push updates instantly.

Benefits:

1. Near real-time UI updates for all list members
2. Less network overhead than frequent polling
3. Better collaborative UX (changes appear immediately)

## Why STOMP Is Used On Top of WebSocket

Raw WebSocket gives only low-level frames. STOMP adds a simple messaging protocol with destinations and headers.

Benefits in this project:

1. Topic-based subscriptions like `/topic/lists/{listId}`
2. Standard CONNECT/SUBSCRIBE/MESSAGE frame flow for clients
3. Easy auth header handling in CONNECT frame
4. Clean integration with Spring messaging (`SimpMessagingTemplate`, broker config)

## Summary Of Implemented Changes

### 1) Dependencies

- Added `spring-boot-starter-websocket` in `pom.xml`

### 2) WebSocket + Broker Setup

Files:

- `src/main/java/com/sharecart/sharecart/realtime/config/WebSocketConfig.java`

Behavior:

1. WebSocket endpoint: `/ws`
2. Broker prefix: `/topic`
3. App prefix: `/app`
4. Inbound channel interceptor registered for phase 2 security

### 3) Realtime Event Model

Files:

- `src/main/java/com/sharecart/sharecart/realtime/dto/ListItemRealtimeEvent.java`

Payload fields:

1. `eventType` (`ITEM_ADDED`, `ITEM_UPDATED`, `ITEM_DELETED`)
2. `listId`
3. `item` (same shape as `ItemResponse`)
4. `occurredAt`

### 4) Publisher Service

Files:

- `src/main/java/com/sharecart/sharecart/realtime/service/ListRealtimePublisher.java`
- `src/main/java/com/sharecart/sharecart/realtime/service/impl/ListRealtimePublisherImpl.java`

Behavior:

1. Publishes to `/topic/lists/{listId}`
2. Sends events after DB transaction commit to avoid publishing rolled-back changes

### 5) Item Service Integration

Files:

- `src/main/java/com/sharecart/sharecart/item/service/impl/ItemServiceImpl.java`

Behavior:

1. `addItem` publishes `ITEM_ADDED`
2. `updateItem` publishes `ITEM_UPDATED`
3. `deleteItem` publishes `ITEM_DELETED`

### 6) HTTP Security Adjustment

Files:

- `src/main/java/com/sharecart/sharecart/common/security/SecurityConfig.java`

Behavior:

1. Allows WebSocket handshake path `/ws/**`
2. Keeps REST API JWT protection unchanged

### 7) Phase 2 Realtime Security

Files:

- `src/main/java/com/sharecart/sharecart/realtime/security/StompAuthChannelInterceptor.java`
- `src/main/java/com/sharecart/sharecart/shoppinglist/service/ListAccessService.java`
- `src/main/java/com/sharecart/sharecart/shoppinglist/service/impl/ListAccessServiceImpl.java`
- `src/main/java/com/sharecart/sharecart/shoppinglist/repository/ListMemberRepository.java`
- `src/main/java/com/sharecart/sharecart/shoppinglist/repository/ShoppingListRepository.java`

Behavior:

1. STOMP CONNECT requires `Authorization: Bearer <jwt>`
2. JWT is validated before allowing messaging session
3. STOMP SUBSCRIBE allowed only for destination `/topic/lists/{listId}`
4. User must be owner or member of the list to subscribe
5. Unauthorized subscriptions are rejected server-side

## Flutter Team Instructions (Copy This)

1. Login via REST and keep JWT token.
2. Connect STOMP websocket to `/ws`.
3. Send CONNECT header: `Authorization: Bearer <jwt>`.
4. Subscribe to `/topic/lists/{listId}` only for lists user belongs to.
5. Handle events:
   - `ITEM_ADDED` -> insert item
   - `ITEM_UPDATED` -> replace item by id
   - `ITEM_DELETED` -> remove item by id
6. On disconnect/error, reconnect and re-fetch list via REST.

## Related Docs

1. `docs/realtime-websocket-sync.md`
2. `docs/api-input-output-reference.md`
3. `docs/flutter-backend-integration.md`