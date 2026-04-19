# Render Deployment Troubleshooting - ShareCart

This document captures the deployment errors faced while deploying ShareCart to Render and the fixes applied.

## 1) Docker build error: target folder not found

### Error

```text
COPY target/*.jar app.jar
ERROR: lstat /target: no such file or directory
```

### Cause

The Dockerfile used multi-stage build, but `COPY target/*.jar app.jar` tries to copy from the host build context, not from the previous build stage.

### Solution

Use `--from=build` to copy the jar from the Maven build stage:

```dockerfile
COPY --from=build /app/target/*.jar app.jar
```

---

## 2) Maven compile error on Render: Java version mismatch

### Error

```text
Fatal error compiling: error: release version 21 not supported
```

### Cause

Project is configured for Java 21 (`java.version=21` in pom.xml), but Docker images were using Java 17.

### Solution

Update both Docker stages to Java 21:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
...
FROM eclipse-temurin:21-jdk-alpine
```

---

## 3) Runtime startup error: JwtUtil bean creation failed

### Error

```text
BeanCreationException: Error creating bean with name 'jwtUtil'
```

### Cause

JWT properties were only present in `application-dev.properties`. Render runs with `prod` profile, so `app.jwt.secret` and `app.jwt.expiration-ms` were missing for `JwtUtil` initialization.

### Solution

Move JWT properties to common `application.properties` and allow environment overrides:

```properties
app.jwt.secret=${APP_JWT_SECRET:sharecart-super-secret-jwt-key-replace-this-before-deploying-to-prod!}
app.jwt.expiration-ms=${APP_JWT_EXPIRATION_MS:86400000}
```

Important:
- `APP_JWT_SECRET` must be at least 32 characters for HMAC-SHA256.

---

## 4) Runtime DB error: transaction aborted / prepared statement already exists

### Errors

```text
SQLState: 25P02
ERROR: current transaction is aborted, commands ignored until end of transaction block
org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists
```

### Cause

Prepared statement conflicts with pooled PostgreSQL connections (common with pooler/PgBouncer style endpoints).

### Solution

Set PostgreSQL driver compatibility properties in `application-prod.properties`:

```properties
spring.datasource.hikari.data-source-properties.prepareThreshold=0
spring.datasource.hikari.data-source-properties.preferQueryMode=simple
```

---

## 5) Final Dockerfile used for Render

```dockerfile
# Step 1: Build the app
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the app
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

## 6) Required Render environment variables

Set these in Render service settings:

```text
SPRING_PROFILES_ACTIVE=prod
PORT=8080
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
APP_JWT_SECRET=... (>= 32 chars)
APP_JWT_EXPIRATION_MS=86400000   (optional)
```

---

## 7) Quick verify checklist after deploy

- Build passes with Java 21 image.
- Application starts without BeanCreationException for JwtUtil.
- No PostgreSQL prepared statement conflict in logs.
- Health endpoint / startup logs show app is listening on Render port.
