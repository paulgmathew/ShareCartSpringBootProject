Refactor the Spring Boot price confirmation workflow to move all store resolution logic from the Flutter client into the backend.

## Existing Implementation

The current endpoint

POST /api/v1/prices/confirm

expects

```java
UUID storeId
```

inside ConfirmPriceRequest.

The Flutter application currently must know the storeId before confirming prices.

This design should be removed.

Flutter should never know database identifiers.

Flutter should only send information it actually observes.

The backend should own all store resolution and creation.

---

## New Request Contract

Replace ConfirmPriceRequest with:

```java
public record ConfirmPriceRequest(

    UUID captureId,

    String scanType,

    String source,

    OffsetDateTime capturedAt,

    @Valid
    StoreInfoRequest store,

    @Valid
    List<ConfirmPriceItemRequest> items

)
```

Create

StoreInfoRequest

Fields

* name
* address
* latitude
* longitude

Remove

storeId

from ConfirmPriceRequest.

---

## Store Resolution

Create a StoreResolverService.

Method:

Store resolve(StoreInfoRequest request)

Responsibilities:

1. Search existing stores.

2. Match by name.

3. Compare geographic distance.

4. If an appropriate store exists:

Return existing Store.

5. Otherwise

Create a new Store entity.

Persist it.

Return the new Store.

The Flutter client must never perform this logic.

---

## PriceService Changes

Refactor confirmPrice()

Current implementation:

Store store =
storeRepository.findById(request.storeId())

Replace with

Store store =
storeResolverService.resolve(request.store())

Use the returned Store entity for every ItemPrice.

---

## Transaction Boundary

The entire confirmPrice() operation must remain inside one transaction.

The transaction should include:

Resolve or create Store

↓

Persist ItemPrice entities

↓

Commit

If any ItemPrice fails to save

Rollback everything including newly created Store.

---

## Bulk Processing

Always process the request using

request.items()

Remove the legacy logic supporting

itemName

price

unit

at the root of ConfirmPriceRequest.

The request should always contain an items array.

Even a single confirmed item should be represented as a list containing one element.

---

## Controller

The controller should remain almost unchanged.

Only accept the updated ConfirmPriceRequest.

No controller logic for store creation.

No controller logic for store lookup.

All business logic belongs in the service layer.

---

## Repository

Create any repository methods needed for store lookup.

Example:

findByNameIgnoreCase()

or

findNearbyStore()

Do not expose lookup logic to the controller.

---

## Future Design

Design StoreResolverService so it can later support:

* fuzzy name matching

* geospatial queries

* Google Places integration

without changing the controller or API contract.

---

## Final Architecture

Flutter

↓

POST /prices/confirm

↓

PriceService

↓

StoreResolverService

↓

Find or Create Store

↓

Save ItemPrices

↓

Commit Transaction

Flutter never knows the Store ID.

Spring Boot owns store identity.

Generate clean production-quality code following existing package structure, coding conventions, exception handling, logging, and transaction management already present in the project.
