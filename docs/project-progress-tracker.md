# ShareCart Project Progress Tracker

This tracker uses progressive disclosure: start with the status board, then expand into technical detail only when needed.

## 1. Status Board

### ✅ Completed
- User registration flow
- Email verification flow
- Resend verification email flow
- JWT authentication and protected routes
- Shopping list creation and membership logic
- Invite-link generation and acceptance flow
- Item create/update/delete flow
- Price capture and confirmation workflow
- Price comparison and history retrieval
- Store lookup and deduplication
- Realtime item update hooks

### 🔄 In Progress
- Final deep-link email verification flow for Flutter app
- Backend/mobile integration polish
- Documentation cleanup and project tracking

### ⏭️ Next Up
- Review feature completeness across all modules
- Validate production config and deployment values
- Expand testing for edge cases and auth flows
- Finalize mobile app integration polish

---

## 2. Last Change Summary

### Most recent update
- Branch: `feature/emailSetupForAccountCreation`
- Commit: `f130252` — `adding email`

### Files changed
- [src/main/resources/application-dev.properties](../src/main/resources/application-dev.properties)
- [src/main/resources/application-prod.properties](../src/main/resources/application-prod.properties)

### What changed
The key update was switching the verification link to a mobile deep link:

- Dev value: `sharecart://verify-email?token=`

This means email verification is now aligned with a Flutter app flow, where the mobile app reads the token from the deep link and then calls the backend verify endpoint.

---

## 3. Feature Progress

### Authentication
Status: Completed for app use

Included:
- register
- verify email
- resend verification
- login
- JWT-protected APIs

Relevant code area:
- `src/main/java/com/sharecart/sharecart/auth/`

### Shopping Lists
Status: Completed

Included:
- create list
- get own lists
- invite members
- invite-link preview
- accept invite

### Items
Status: Completed

Included:
- add item
- update item
- delete item

### Price Engine
Status: Completed

Included:
- capture price data
- confirm captured data
- compare prices
- retrieve history
- store lookup and deduplication

### Realtime / Sync
Status: Mostly implemented

Current focus:
- item event alignment
- realtime consistency with backend state

### AI API Integration
Status: Documented and scoped

Related docs:
- [docs/sharecart-ai-api-overview.md](sharecart-ai-api-overview.md)
- [docs/sharecart-ai-jwt-authentication-guide.md](sharecart-ai-jwt-authentication-guide.md)

---

## 4. Change Tracking Notes

This repo already follows a layered progression:

1. core backend features were built first
2. email verification was added next
3. mobile deep-link behavior was refined most recently
4. AI integration was documented afterward

This is a healthy progression model for a growing backend app.

---

## 5. Suggested Review Pattern

Use this order when checking project progress:

1. Status board
2. Last change summary
3. Feature progress
4. Relevant docs for the current module
5. Git history for deeper context

This keeps the project understandable without rereading everything.

---

## 6. Best Working Rule

Treat this tracker as the summary layer. Keep raw implementation details in the code and feature docs, and use this file as the quick memory layer for what changed and what is still active.
