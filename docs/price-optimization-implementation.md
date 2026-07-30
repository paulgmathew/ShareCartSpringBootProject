# Price Optimization Implementation Changes

## Overview

This document records all backend changes implemented for the price optimization feature in ShareCart.

Architecture followed:

Controller -> Service -> Repository -> Entity

All new APIs are under `/api/v1`, use JWT auth, UUID identifiers, DTO-based contracts, and central exception handling via `GlobalExceptionHandler`.

No schema migrations were introduced for this feature. Existing tables were used as-is:

- `stores`
- `item_prices`
- `price_captures`
- `store_visits` (reserved for future work, no code path added yet)

---

## New Module Added

New package: `com.sharecart.sharecart.price`

Structure:

```text
price/
  controller/
    PriceController.java
    StoreController.java
  dto/
    ComparePriceRequest.java
    ComparePriceResponse.java
    ConfirmPriceRequest.java
    CreatePriceCaptureRequest.java
    CreatePriceCaptureResponse.java
    CreateStoreRequest.java
    ItemPriceResponse.java
    NearbyStoreResponse.java
    StoreResponse.java
  model/
    ItemPrice.java
    PriceCapture.java
    Store.java
  repository/
    ItemPriceRepository.java
    PriceCaptureRepository.java
    StoreRepository.java
  service/
    PriceService.java
    StoreService.java
    impl/
      PriceServiceImpl.java
      StoreServiceImpl.java
  util/
    HaversineDistanceUtil.java
```

---

## Entity Mapping Implemented

### Store -> `stores`

Mapped fields:

- `id` (UUID, generated)
- `name`
- `address`
- `latitude`
- `longitude`
- `createdAt` -> `created_at`

File: `src/main/java/com/sharecart/sharecart/price/model/Store.java`

### PriceCapture -> `price_captures`

Mapped fields:

- `id` (UUID, generated)
- `rawText` -> `raw_text`
- `imageUrl` -> `image_url`
- `latitude`
- `longitude`
- `userId` -> `user_id`
- `createdAt` -> `created_at`

File: `src/main/java/com/sharecart/sharecart/price/model/PriceCapture.java`

### ItemPrice -> `item_prices`

Mapped fields:

- `id` (UUID, generated)
- `itemName` -> `item_name`
- `normalizedName` -> `normalized_name`
- `store` (`@ManyToOne` -> `store_id`)
- `price` (`BigDecimal`)
- `unit`
- `capturedAt` -> `captured_at`
- `source`
- `createdBy` -> `created_by`
- `createdAt` -> `created_at`

File: `src/main/java/com/sharecart/sharecart/price/model/ItemPrice.java`

---

## Repository Layer Changes

### StoreRepository

File: `src/main/java/com/sharecart/sharecart/price/repository/StoreRepository.java`

Added:

- Bounding-box query (`findByBoundingBox`) using:
  - latitude between `(lat - 0.02)` and `(lat + 0.02)`
  - longitude between `(lon - 0.02)` and `(lon + 0.02)`
- `findByNameIgnoreCase(String name)`
- `findAllByNameIgnoreCase(String name)` (used by de-dup flow)

### ItemPriceRepository

File: `src/main/java/com/sharecart/sharecart/price/repository/ItemPriceRepository.java`

Added:

- `List<ItemPrice> findByNormalizedName(String normalizedName)`
- `Optional<ItemPrice> findTopByNormalizedNameAndStoreIdOrderByCreatedAtDesc(String normalizedName, UUID storeId)`

### PriceCaptureRepository

File: `src/main/java/com/sharecart/sharecart/price/repository/PriceCaptureRepository.java`

- Standard CRUD via `JpaRepository`

---

## Service Layer Changes

### StoreService

Files:

- `src/main/java/com/sharecart/sharecart/price/service/StoreService.java`
- `src/main/java/com/sharecart/sharecart/price/service/impl/StoreServiceImpl.java`

Implemented methods:

1. `findNearbyStores(latitude, longitude)`
- Uses bounding-box repository query
- Computes exact distance in Java with Haversine
- Sorts ascending by distance
- Limits results to top 10

2. `createStoreIfNotExists(name, address, latitude, longitude)`
- Case-insensitive name match search
- For each candidate, distance check via Haversine
- If distance < 200m, returns existing store
- Otherwise creates a new store

3. `resolveStore(...)`
- Internal service-level helper used by price confirmation flow

### PriceService

Files:

- `src/main/java/com/sharecart/sharecart/price/service/PriceService.java`
- `src/main/java/com/sharecart/sharecart/price/service/impl/PriceServiceImpl.java`

Implemented methods:

1. `normalizeItemName(itemName)`
- Lowercase
- Remove special characters
- Collapse multiple spaces
- Trim

Example behavior:

- `Milk (1L)` -> `milk 1l`

2. `createCapture(request, userId)`
- Saves to `price_captures`
- Stores authenticated JWT user as `user_id`
- Returns `captureId`

3. `confirmPrice(request, userId)`
- Validates capture exists (404 if missing)
- Normalizes item name
- Resolves store by `storeName + latitude + longitude` (no `storeId` from client)
- Dedup logic:
  - Finds latest same `normalized_name + store_id`
  - If same price and created within 24 hours, returns existing row
- Otherwise inserts new `item_prices` row:
  - `item_name`
  - `normalized_name`
  - `store_id`
  - `price` (`BigDecimal`)
  - `unit`
  - `source = OCR`
  - `created_by` from JWT
  - `captured_at = now`

4. `comparePrice(request)`
- Normalizes item name
- Fetches all entries by `normalized_name`
- Computes:
  - `lowestPrice`
  - `lowestStoreId`
  - `averagePrice`
  - `totalEntries`
- Returns 404 if no matching entries

---

## Controller Layer Changes

### StoreController

File: `src/main/java/com/sharecart/sharecart/price/controller/StoreController.java`

Endpoints:

1. `GET /api/v1/stores/nearby?lat=..&lon=..`
- Auth required
- Returns nearby stores with computed distance

2. `POST /api/v1/stores`
- Auth required
- Creates or resolves existing store via 200m dedupe rule

### PriceController

File: `src/main/java/com/sharecart/sharecart/price/controller/PriceController.java`

Endpoints:

1. `POST /api/v1/prices/capture`
- Auth required
- Creates a capture and returns `captureId`

2. `POST /api/v1/prices/confirm`
- Auth required
- Confirms parsed OCR price into canonical `item_prices`
- Does not accept `storeId` from client

3. `POST /api/v1/prices/compare`
- Auth required
- Returns aggregate price comparison

JWT user extraction:

- User ID is read from `SecurityContextHolder`
- `created_by` is never accepted from request payload

---

## DTO Contracts Added

Files in `src/main/java/com/sharecart/sharecart/price/dto/`:

- `CreateStoreRequest`
- `StoreResponse`
- `NearbyStoreResponse`
- `CreatePriceCaptureRequest`
- `CreatePriceCaptureResponse`
- `ConfirmPriceRequest`
- `ItemPriceResponse`
- `ComparePriceRequest`
- `ComparePriceResponse`

Validation annotations are applied on request DTOs for required fields and valid numeric constraints.

---

## Utility Added

### HaversineDistanceUtil

File: `src/main/java/com/sharecart/sharecart/price/util/HaversineDistanceUtil.java`

Purpose:

- Compute geographic distance in meters between two latitude/longitude points
- Used for nearby store sorting and store de-duplication

---

## Error Handling Updates

File updated: `src/main/java/com/sharecart/sharecart/common/exception/GlobalExceptionHandler.java`

Added handler:

- `IllegalArgumentException` -> HTTP 400

This supports business validation failures such as invalid normalized item names.

Other existing handlers continue to cover:

- 404 (`ResourceNotFoundException`)
- 409 (`IllegalStateException`)
- 403 (`AccessDeniedException`)
- validation failures (400)

---

## Security & Conventions Compliance

Implemented as requested:

- JWT required for all new price/store endpoints
- UUID keys used throughout
- DTO-based request/response contracts
- Layered architecture maintained
- Standard HTTP statuses used: 200, 201, 400, 403, 404, 409
- `BigDecimal` used for all price values
- Matching always performed against `normalized_name`
- Duplicate stores prevented within 200 meters

---

## Postman Quick Test Sequence

Use this flow to validate the end-to-end feature in minutes.

Prerequisites:

1. app running on `http://localhost:8080`
2. valid JWT token from `/api/v1/auth/login`
3. set a Postman collection variable `baseUrl = http://localhost:8080/api/v1`
4. set a Postman variable `token = <jwt>`

Common header for protected routes:

```text
Authorization: Bearer {{token}}
Content-Type: application/json
```

### Step 1: Create capture

Request:

- Method: `POST`
- URL: `{{baseUrl}}/prices/capture`
- Body:

```json
{
  "rawText": "Milk 1L $3.49",
  "imageUrl": "https://example.com/receipt-1.jpg",
  "latitude": 32.99,
  "longitude": -96.70
}
```

Expected:

- `201 Created`
- response contains `captureId`

Save `captureId` for next step.

### Step 2: Confirm price

Request:

- Method: `POST`
- URL: `{{baseUrl}}/prices/confirm`
- Body:

```json
{
  "captureId": "<captureId-from-step-1>",
  "itemName": "Milk (1L)",
  "price": 3.49,
  "unit": "1L",
  "storeName": "Walmart",
  "latitude": 32.99,
  "longitude": -96.70
}
```

Expected:

- `200 OK`
- response includes:
  - `normalizedName` (for this sample, should be `milk 1l`)
  - `storeId`
  - `price`

### Step 3: Compare price

Request:

- Method: `POST`
- URL: `{{baseUrl}}/prices/compare`
- Body:

```json
{
  "itemName": "Milk 1L"
}
```

Expected:

- `200 OK`
- response contains:
  - `lowestPrice`
  - `lowestStoreId`
  - `averagePrice`
  - `totalEntries`

### Step 4: Nearby stores

Request:

- Method: `GET`
- URL: `{{baseUrl}}/stores/nearby?lat=32.99&lon=-96.70`

Expected:

- `200 OK`
- list sorted by `distanceMeters` (ascending)

### Optional Step 5: De-dup verification

Repeat Step 2 with the exact same `itemName`, `price`, and nearby location within 24 hours.

Expected:

- `200 OK`
- existing entry returned (no duplicate insert)

### Common failure checks

1. `400 Bad Request`
   - missing `latitude`/`longitude`
   - `price <= 0`
   - blank `itemName`

2. `403 Forbidden`
   - missing or invalid JWT

3. `404 Not Found`
   - invalid `captureId` in confirm
   - no data for compare item

4. `409 Conflict`
   - reserved for business conflicts handled as `IllegalStateException`

---

## Build Validation

The project compiles successfully after these changes using:

```bash
./mvnw compile -q
```
