# ShareCart Email User Registration Verification Guide

## Purpose

This document captures all changes made to support email-based user registration with verification in the ShareCart Spring Boot backend.

## What Changed

### 1. Registration Flow

Before:
- Register endpoint created the user and returned JWT immediately.

Now:
- Register endpoint creates user in unverified state.
- Verification token and expiry are stored on the user.
- Verification email is sent via Mailtrap API.
- Register endpoint returns a message asking the user to verify email.

### 2. Login Flow

Before:
- Login issued JWT for valid email/password.

Now:
- Login checks if email is verified.
- If not verified, login is blocked with conflict error.
- JWT is issued only after successful verification.

### 3. New Email Verification Endpoints

- GET /api/v1/auth/verify-email?token={token}
- POST /api/v1/auth/resend-verification

## Mail Delivery Mode

Mail sending uses Mailtrap Email API token mode (not SMTP host/port/username/password).

## File Changes

### Auth DTOs

- Added: src/main/java/com/sharecart/sharecart/auth/dto/RegisterResponse.java
- Added: src/main/java/com/sharecart/sharecart/auth/dto/VerifyEmailResponse.java
- Added: src/main/java/com/sharecart/sharecart/auth/dto/ResendVerificationRequest.java

### Auth Service Contracts

- Updated: src/main/java/com/sharecart/sharecart/auth/service/AuthService.java
  - register now returns RegisterResponse
  - added verifyEmail(String token)
  - added resendVerificationEmail(ResendVerificationRequest request)

### Auth Service Implementation

- Updated: src/main/java/com/sharecart/sharecart/auth/service/impl/AuthServiceImpl.java
  - register creates unverified user and sends verification email
  - login blocks unverified users
  - verifyEmail validates token and marks user verified
  - resendVerificationEmail regenerates token and resends email

### Email Sending Service

- Added: src/main/java/com/sharecart/sharecart/auth/service/VerificationEmailService.java
- Updated/Implemented: src/main/java/com/sharecart/sharecart/auth/service/impl/VerificationEmailServiceImpl.java
  - uses Mailtrap API endpoint
  - authenticates with Bearer MAILTRAP_API_TOKEN
  - sends both plain text and HTML email payload
  - loads HTML template from resources

### Email Template

- Added: src/main/resources/templates/emails/verification-email.html

### Controller Endpoints

- Updated: src/main/java/com/sharecart/sharecart/auth/controller/AuthController.java
  - POST /api/v1/auth/register returns RegisterResponse
  - GET /api/v1/auth/verify-email
  - POST /api/v1/auth/resend-verification

### User Entity and Repository

- Updated: src/main/java/com/sharecart/sharecart/user/model/User.java
  - emailVerified
  - emailVerificationToken
  - emailVerificationExpiresAt
  - emailVerifiedAt

- Updated: src/main/java/com/sharecart/sharecart/user/repository/UserRepository.java
  - findByEmailVerificationToken(String token)

### Config Files

- Updated: src/main/resources/application-dev.properties
- Updated: src/main/resources/application-prod.properties

### Dependency

- Updated: pom.xml
- Current mode does not require spring-boot-starter-mail because sending is through Mailtrap HTTP API.

## Database Requirements

The backend expects the following user table fields to exist:

- email_verified boolean not null default false
- email_verification_token varchar(255) nullable unique
- email_verification_expires_at timestamp nullable
- email_verified_at timestamp nullable

Recommended index:
- index on email_verification_expires_at

## Environment Variables

### Required

- MAILTRAP_API_TOKEN
- APP_MAIL_FROM
- APP_MAIL_VERIFY_BASE_URL

### Optional

- MAILTRAP_SEND_ENDPOINT (default: https://send.api.mailtrap.io/api/send)
- APP_MAIL_FROM_NAME (default: ShareCart)
- APP_MAIL_VERIFICATION_EXPIRY_MINUTES (default: 30)
- APP_MAILTRAP_CATEGORY (default: Account Verification)

## API Response Shapes

### Register Success (201)

{
  "message": "Registration successful. Please check your email to verify your account.",
  "email": "user@example.com",
  "emailVerified": false
}

### Verify Success (200)

{
  "message": "Email verified successfully. You can now log in.",
  "email": "user@example.com",
  "emailVerified": true
}

### Resend Success (200)

{
  "message": "Verification email sent. Please check your inbox.",
  "email": "user@example.com",
  "emailVerified": false
}

## Mailtrap Payload (Simplified)

{
  "from": {
    "email": "no-reply@sharecart.app",
    "name": "ShareCart"
  },
  "to": [
    { "email": "user@example.com" }
  ],
  "subject": "Verify your ShareCart account",
  "text": "Plain text body",
  "html": "HTML body",
  "category": "Account Verification"
}

## Validation Done

- Maven compile passed after implementation.
- Auth service unit tests passed for register, verify, resend, and login-gating behavior.

## Notes

- This implementation verifies account ownership by email before allowing login JWT issuance.
- If needed, migration to token-hash storage can be added later for stronger token-at-rest security.
