# ShareCart Spring Boot API Notes

## Current API Endpoints

### Auth (Email Verification Flow)

- `POST /api/v1/auth/register`
- `GET /api/v1/auth/verify-email?token={token}`
- `POST /api/v1/auth/resend-verification`
- `POST /api/v1/auth/login`

Flow notes:

- Register creates the account in unverified state and sends a verification email.
- Verify endpoint marks the account as verified.
- Login returns JWT only for verified accounts.

### Catalog

- `POST /api/v1/catalog/items`
- `GET /api/v1/catalog/items`
- `GET /api/v1/catalog/items/{id}`

### Users

- `GET /api/v1/users/me/location`
- `PATCH /api/v1/users/me/location`

### Prices

- `POST /api/v1/prices/capture`
- `POST /api/v1/prices/confirm`
- `POST /api/v1/prices/compare`
- `GET /api/v1/prices/best-store/{canonicalItemId}`
- `GET /api/v1/prices/best-prices`
- `GET /api/v1/prices/history`
- `DELETE /api/v1/prices/history/{id}`

### Canonical Item Catalog

Endpoint group:

```text
POST /api/v1/catalog/items
GET /api/v1/catalog/items
GET /api/v1/catalog/items/{id}
```

### User Home Location

Endpoint group:

```text
GET /api/v1/users/me/location
PATCH /api/v1/users/me/location
```

### Delete a Price History Entry

Endpoint:

```text
DELETE /api/v1/prices/history/{id}
Authorization: Bearer <token>
```

Curl example:

```bash
curl -X DELETE "http://localhost:8080/api/v1/prices/history/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" \
  -H "Authorization: Bearer <token>"
```

### Important API Notes

- Only the authenticated user who created the price history entry can delete it.
- If the entry exists but belongs to another user, the API returns `403 Forbidden`.
- If the entry does not exist, the API returns `404 Not Found`.
- On success, the API returns `204 No Content` with an empty body.
- Error responses follow the existing common exception model with `status` and `message` fields.

### Price Summary View

Endpoint:

```text
GET /api/v1/prices/best-prices
Authorization: Bearer <token>
```

Notes:

- Returns one row per canonical item the authenticated user has captured prices for.
- Rows without a canonical item are excluded.
- Each row includes the canonical item id, item name, lowest price, and the store for that lowest price.