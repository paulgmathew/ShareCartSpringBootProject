# ShareCart Realtime Sync (WebSocket + STOMP)

## Goal

Enable live list updates so all members see item changes instantly without polling.

## Backend Contract (Implemented)

1. STOMP WebSocket endpoint: `/ws`
2. Topic destination pattern: `/topic/lists/{listId}`
3. Event types: `ITEM_ADDED`, `ITEM_UPDATED`, `ITEM_DELETED`
4. Events are published only after transaction commit

## Phase 2 Security (Implemented)

1. STOMP CONNECT must include `Authorization: Bearer <jwt>` in CONNECT headers.
2. STOMP SUBSCRIBE to `/topic/lists/{listId}` is accepted only if user is owner/member of that list.

If either check fails, the server rejects the STOMP frame.

## Why This Uses STOMP + WebSocket

1. WebSocket gives server push, so collaborators get updates instantly.
2. STOMP provides a standard messaging contract with destinations, headers, and frame types.
3. STOMP works well with Spring messaging abstractions and client libraries.
4. Topic model (`/topic/lists/{listId}`) maps naturally to collaborative list screens.

## Flutter Setup

Recommended package:

```yaml
dependencies:
  stomp_dart_client: ^2.0.0
```

## URL Mapping For Flutter Environments

Use websocket URL matching your runtime:

1. Android emulator: `ws://10.0.2.2:8080/ws`
2. iOS simulator: `ws://127.0.0.1:8080/ws`
3. Flutter web (same machine): `ws://localhost:8080/ws`
4. Physical device: `ws://<your-local-ip>:8080/ws`
5. Render/Prod: `wss://<your-render-domain>/ws`

## Event Payload

Every message sent to `/topic/lists/{listId}` has this shape:

```json
{
  "eventType": "ITEM_ADDED",
  "listId": "22222222-2222-2222-2222-222222222222",
  "item": {
    "id": "44444444-4444-4444-4444-444444444444",
    "listId": "22222222-2222-2222-2222-222222222222",
    "name": "Milk",
    "quantity": "2",
    "isCompleted": false,
    "category": "Dairy",
    "createdBy": "11111111-1111-1111-1111-111111111111",
    "createdAt": "2026-03-27T09:00:00",
    "updatedAt": "2026-03-27T09:00:00"
  },
  "occurredAt": "2026-03-27T09:00:00Z"
}
```

## Flutter STOMP Client Template

```dart
import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

class ListRealtimeEvent {
  final String eventType;
  final String listId;
  final Map<String, dynamic> item;
  final DateTime occurredAt;

  ListRealtimeEvent({
    required this.eventType,
    required this.listId,
    required this.item,
    required this.occurredAt,
  });

  factory ListRealtimeEvent.fromJson(Map<String, dynamic> json) {
    return ListRealtimeEvent(
      eventType: json['eventType'] as String,
      listId: json['listId'] as String,
      item: (json['item'] as Map).cast<String, dynamic>(),
      occurredAt: DateTime.parse(json['occurredAt'] as String),
    );
  }
}

class ShareCartRealtimeClient {
  final String wsUrl;
  final Future<String?> Function() getJwtToken;
  final Future<void> Function(String listId) onNeedResync;

  StompClient? _client;
  final Map<String, StompUnsubscribe> _subscriptions = {};
  final _eventController = StreamController<ListRealtimeEvent>.broadcast();

  Stream<ListRealtimeEvent> get events => _eventController.stream;

  ShareCartRealtimeClient({
    required this.wsUrl,
    required this.getJwtToken,
    required this.onNeedResync,
  });

  Future<void> connect() async {
    final token = await getJwtToken();
    if (token == null || token.isEmpty) {
      throw StateError('Missing JWT token for STOMP CONNECT');
    }

    _client = StompClient(
      config: StompConfig(
        url: wsUrl,
        stompConnectHeaders: {'Authorization': 'Bearer $token'},
        webSocketConnectHeaders: {'Authorization': 'Bearer $token'},
        reconnectDelay: const Duration(seconds: 3),
        onConnect: (_) {},
        onStompError: (frame) {
          // Server rejected CONNECT/SUBSCRIBE (auth or access issue)
          print('STOMP error: ${frame.body}');
        },
        onWebSocketError: (error) {
          print('WebSocket error: $error');
        },
        onDisconnect: (_) {
          // On reconnect, caller can trigger REST re-sync per opened list
        },
      ),
    );

    _client!.activate();
  }

  Future<void> subscribeToList(String listId) async {
    final client = _client;
    if (client == null) throw StateError('STOMP client not connected');

    if (_subscriptions.containsKey(listId)) return;

    final destination = '/topic/lists/$listId';
    _subscriptions[listId] = client.subscribe(
      destination: destination,
      callback: (frame) {
        final body = frame.body;
        if (body == null || body.isEmpty) return;

        try {
          final jsonMap = jsonDecode(body) as Map<String, dynamic>;
          final event = ListRealtimeEvent.fromJson(jsonMap);
          _eventController.add(event);
        } catch (_) {
          // If malformed message appears, fall back to REST sync.
          unawaited(onNeedResync(listId));
        }
      },
    );
  }

  void unsubscribeFromList(String listId) {
    _subscriptions.remove(listId)?.call();
  }

  Future<void> dispose() async {
    for (final unsub in _subscriptions.values) {
      unsub();
    }
    _subscriptions.clear();
    _client?.deactivate();
    await _eventController.close();
  }
}
```

## UI State Update Logic

On each `ListRealtimeEvent`:

1. `ITEM_ADDED`: append `item` if not present
2. `ITEM_UPDATED`: find item by `id` and replace
3. `ITEM_DELETED`: remove item by `id`

If any mismatch occurs, call REST `GET /api/v1/lists/{id}` and replace full state.

## Minimal Integration Sequence

1. Login and store JWT.
2. Load list screen using REST `GET /api/v1/lists/{id}`.
3. Connect STOMP with `Authorization: Bearer <jwt>`.
4. Subscribe to `/topic/lists/{listId}`.
5. Merge realtime events into state.
6. On reconnect, trigger REST resync.

## Paste-Ready Prompt For Copilot In Flutter Project

```md
Implement a ShareCart realtime service using stomp_dart_client.

Backend contract:
- WebSocket endpoint: /ws
- STOMP CONNECT header required: Authorization: Bearer <jwt>
- Subscribe destination: /topic/lists/{listId}
- Event types: ITEM_ADDED, ITEM_UPDATED, ITEM_DELETED
- Event payload fields: eventType, listId, item, occurredAt

Requirements:
1. Build a singleton realtime client with connect, subscribeToList, unsubscribeFromList, dispose.
2. Read JWT from secure storage and send it in STOMP CONNECT headers.
3. Expose a Stream<ListRealtimeEvent> for UI layers.
4. On ITEM_ADDED/ITEM_UPDATED/ITEM_DELETED, update list state by item id.
5. On parse errors or reconnects, re-fetch GET /api/v1/lists/{id} to resync.
6. Provide robust reconnect handling and avoid duplicate subscriptions.
```
