# ShareCart Spring Boot Project

Backend service for the ShareCart mobile application.

## Tech Stack

- Java 25
- Spring Boot 4.0.4
- Spring Web
- Spring Data JPA
- PostgreSQL (local development)
- Hibernate 7
- Lombok

## Architecture

The project follows a clean layered architecture:

- Controller -> exposes REST APIs
- Service -> business logic
- Repository -> data access with Spring Data JPA
- DB -> PostgreSQL database

## Project Structure

src/main/java/com/sharecart/sharecart

- cart/controller -> REST controllers
- cart/service -> service contracts
- cart/service/impl -> service implementations
- cart/repository -> repository interfaces
- cart/model -> JPA entities
- cart/dto -> request/response models
- common/exception -> centralized exception handling

## Database Setup (PostgreSQL)

Make sure PostgreSQL is installed and running locally on port **5432**.

Create the database before running the application:

```sql
CREATE DATABASE sharecartdb;
```

The application connects using the following default credentials (see `src/main/resources/application.properties`):

| Property | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | sharecartdb |
| Username | postgres |
| Password | docker1 |

To customise, update the values in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sharecartdb
spring.datasource.username=postgres
spring.datasource.password=docker1
```

## Run Locally

Use Maven wrapper:

```bash
./mvnw spring-boot:run
```

## Default URLs

- Application: http://localhost:8080

## API Endpoints

Base path: `/api/v1`

### Items

- `GET    /api/v1/items`
- `GET    /api/v1/items/{id}`
- `POST   /api/v1/items`
- `PUT    /api/v1/items/{id}`
- `DELETE /api/v1/items/{id}`

### Shopping Lists

- `GET    /api/v1/shopping-lists`
- `GET    /api/v1/shopping-lists/{id}`
- `POST   /api/v1/shopping-lists`
- `PUT    /api/v1/shopping-lists/{id}`
- `DELETE /api/v1/shopping-lists/{id}`
