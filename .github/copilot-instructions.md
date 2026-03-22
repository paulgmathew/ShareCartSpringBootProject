# GitHub Copilot Instructions — ShareCart Spring Boot Project

## Project Overview

ShareCart is a Spring Boot REST API backend for a shared shopping cart mobile application. It enables users to create shopping lists, invite members, and collaboratively manage items.

- **Language**: Java 21
- **Framework**: Spring Boot 4.0.4
- **Database**: PostgreSQL (local, port 5432, database `sharecartdb`)
- **ORM**: Spring Data JPA / Hibernate 7
- **Build Tool**: Maven (use `./mvnw`)
- **Base URL**: `http://localhost:8080`
- **API Version Prefix**: `/api/v1`

---

## Architecture

This project follows a **clean layered architecture** with strict separation of concerns:

```
Controller  →  Service (interface + impl)  →  Repository  →  Database
                    ↕
                  DTOs (Request / Response)
```

All new features must follow this layering. Never bypass layers (e.g., no direct repository calls from a controller).

### Module Structure

Each domain feature is a self-contained module under `com.sharecart.sharecart.{module}`:

```
{module}/
  controller/       # REST controllers
  service/          # Service interfaces
  service/impl/     # Service implementations
  repository/       # Spring Data JPA repositories
  model/            # JPA entities
  dto/              # Request and response records
```

**Existing modules**: `item`, `shoppinglist`, `user`  
**Shared code**: `common/exception/`

---

## Coding Conventions

### Naming

| Artifact | Convention | Example |
|---|---|---|
| Package | `com.sharecart.sharecart.{module}.{layer}` | `com.sharecart.sharecart.item.controller` |
| Controller class | `{Entity}Controller` | `ItemController` |
| Service interface | `{Entity}Service` | `ItemService` |
| Service implementation | `{Entity}ServiceImpl` | `ItemServiceImpl` |
| Repository | `{Entity}Repository` | `ItemRepository` |
| Entity/Model | `{Entity}` (singular PascalCase) | `Item`, `ShoppingList` |
| Create DTO | `Create{Entity}Request` (Java Record) | `CreateItemRequest` |
| Update DTO | `Update{Entity}Request` (Java Record) | `UpdateItemRequest` |
| Response DTO | `{Entity}Response` (Java Record) | `ItemResponse` |
| DB table | `snake_case` plural | `shopping_lists`, `list_members` |
| DB column | `snake_case` | `list_id`, `created_by`, `is_completed` |
| Java fields | `camelCase` | `createdBy`, `isCompleted` |
| Controller methods | action-based camelCase | `createList()`, `addItem()`, `inviteUser()` |

### Java Records for DTOs

All request and response DTOs must be **Java Records** (immutable, no Lombok needed):

```java
// Request
public record CreateItemRequest(
    @NotBlank(message = "Item name is required") String name,
    UUID shoppingListId,
    UUID addedBy
) {}

// Response
public record ItemResponse(
    UUID id,
    String name,
    boolean isCompleted,
    UUID shoppingListId,
    LocalDateTime createdAt
) {}
```

### JPA Entities (Models)

All entities must use:
- `@Entity`, `@Table(name = "table_name")`
- UUID primary key: `@Id`, UUID type, generated via `@PrePersist`
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- Timestamps managed by `@PrePersist` / `@PreUpdate`
- Column constraints explicitly declared: `nullable = false`, `length = N`

```java
@Entity
@Table(name = "items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Item {

    @Id
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Builder.Default
    private boolean isCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private ShoppingList shoppingList;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Controllers

- Annotate with `@RestController`, `@RequestMapping("/api/v1/{resource}")`, `@RequiredArgsConstructor`
- Use `@Valid` on all `@RequestBody` parameters
- Return `ResponseEntity<T>` with explicit HTTP status codes:
  - `201 Created` for POST (with resource in body)
  - `200 OK` for GET/PUT
  - `204 No Content` for DELETE
- Never add business logic to controllers — delegate entirely to the service layer

```java
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.createItem(request));
    }
}
```

### Services

- Define a service **interface** first, then implement it in `service/impl/`
- Annotate implementations with `@Service`, `@RequiredArgsConstructor`, `@Transactional`
- All operations that write to the database must be `@Transactional`
- Map entities to DTOs within the service layer — never expose raw entities from controllers

```java
public interface ItemService {
    ItemResponse createItem(CreateItemRequest request);
    ItemResponse updateItem(UUID id, UpdateItemRequest request);
    void deleteItem(UUID id);
}

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    // ...
}
```

### Repositories

- Extend `JpaRepository<Entity, UUID>`
- Define custom query methods using Spring Data derived query conventions
- Avoid `@Query` JPQL unless derived methods cannot express the query

```java
public interface ItemRepository extends JpaRepository<Item, UUID> {
    List<Item> findByShoppingListId(UUID listId);
}
```

### Exception Handling

- Throw `ResourceNotFoundException` (extends `RuntimeException`) when entities are not found
- Business rule violations throw `IllegalStateException`
- All exceptions are handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Never return error details directly from controllers or services; let the global handler format the response

```java
// Throw in service
itemRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
```

### Dependency Injection

Always use **constructor injection** via Lombok's `@RequiredArgsConstructor` with `private final` fields. Never use field injection (`@Autowired`).

---

## Validation

Use **Jakarta Validation** annotations on DTO records:

| Annotation | Use Case |
|---|---|
| `@NotBlank` | Required string fields (name, title, etc.) |
| `@NotNull` | Required object/UUID fields |
| `@Size` | String length constraints |
| `@Valid` | Trigger nested validation in controller params |

Always provide meaningful `message` attributes:
```java
@NotBlank(message = "List name is required")
```

---

## Database

- **Engine**: PostgreSQL 15+ on `localhost:5432`
- **Database name**: `sharecartdb`
- **User**: `postgres`
- **DDL auto**: `update` (Hibernate auto-syncs schema on startup)
- All primary keys are `UUID` generated in `@PrePersist`
- Use `FetchType.LAZY` for all `@ManyToOne` / `@OneToMany` relationships
- Unique constraints declared via `@Table(uniqueConstraints = ...)`

### Relationship Patterns

```java
// ManyToOne (owning side)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "list_id", nullable = false)
private ShoppingList shoppingList;

// OneToMany (inverse side)
@OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Item> items = new ArrayList<>();
```

---

## API Design Guidelines

- All endpoints are prefixed with `/api/v1/`
- Use plural nouns for collection resources: `/lists`, `/items`, `/users`
- Nested resources for ownership: `/api/v1/lists/{listId}/items`
- Standalone operations on a resource: `/api/v1/items/{id}`
- Actions that don't map to CRUD use descriptive sub-paths: `/api/v1/lists/{id}/invite`
- HTTP methods map strictly to intent:
  - `POST` → create
  - `GET` → read
  - `PUT` → replace / full update
  - `PATCH` → partial update
  - `DELETE` → remove

---

## Lombok Usage Reference

| Annotation | Where | Purpose |
|---|---|---|
| `@Getter` / `@Setter` | JPA entities | Auto-generate accessors |
| `@NoArgsConstructor` | JPA entities | Required by JPA spec |
| `@AllArgsConstructor` | JPA entities | For builder compatibility |
| `@Builder` | JPA entities | Fluent construction |
| `@Builder.Default` | Boolean / enum fields | Set default value in builder |
| `@RequiredArgsConstructor` | Controllers, Services | Constructor injection |

**Do not** use Lombok on DTOs — use Java Records instead.

---

## Security Notes

- Do **not** commit real credentials to source control; move secrets to environment variables or a secrets manager before production
- The current `spring.datasource.password` in `application.properties` is for local development only
- No authentication/authorization is configured yet — plan for Spring Security before any production deployment
- Always validate and sanitize all inputs at the controller level using `@Valid`

---

## Testing

- Place tests in `src/test/java/com/sharecart/sharecart/`
- Use `@SpringBootTest` for integration tests
- Unit test services independently, mocking repositories with Mockito
- Name test methods descriptively: `should{Expected}When{Condition}()`

---

## Running the Application

```bash
# Run with Maven wrapper
./mvnw spring-boot:run

# Build JAR
./mvnw clean package

# Run JAR
java -jar target/sharecart-0.0.1-SNAPSHOT.jar
```

**Prerequisite**: PostgreSQL must be running and `sharecartdb` database must exist:
```sql
CREATE DATABASE sharecartdb;
```
