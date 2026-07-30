# ShareCart Spring Boot API Notes

## Current API Endpoints

### Prices

- `POST /api/v1/prices/capture`
- `POST /api/v1/prices/confirm`
- `POST /api/v1/prices/compare`
- `GET /api/v1/prices/history`
- `DELETE /api/v1/prices/history/{id}`

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