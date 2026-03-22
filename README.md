# ShareCart Spring Boot Project

Backend service for the ShareCart mobile application.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- H2 Database (for local development)
- Lombok

## Architecture

The project follows a clean layered architecture:

- Controller -> exposes REST APIs
- Service -> business logic
- Repository -> data access with Spring Data JPA
- DB -> H2 in-memory database (local)

## Project Structure

src/main/java/com/sharecart/sharecart

- cart/controller -> REST controllers
- cart/service -> service contracts
- cart/service/impl -> service implementations
- cart/repository -> repository interfaces
- cart/model -> JPA entities
- cart/dto -> request/response models
- common/exception -> centralized exception handling

## Run Locally

Use Maven wrapper:

```bash
./mvnw spring-boot:run
```

## Default URLs

- Application: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console

## Starter API Endpoints

Base path: /api/v1/cart-items

- GET /api/v1/cart-items
- GET /api/v1/cart-items/{id}
- POST /api/v1/cart-items
- PUT /api/v1/cart-items/{id}
- DELETE /api/v1/cart-items/{id}
