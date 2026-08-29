# Copilot Prompt For Flutter Email Verification Migration

Use this prompt inside the Flutter project workspace.

---

I need you to implement frontend changes for the new backend email-verification auth flow in this Flutter app.

Project architecture summary:
- State management: Provider + ChangeNotifier
- Networking: http
- Token storage: flutter_secure_storage
- Routing/deep link support: app_links
- Layering: Screens -> Providers -> Repositories -> Services -> ApiClient

Please read and use these files as context first:
- docs/flutterappoverview.md
- docs/api-input-output-reference.md
- docs/email-user-registration-verification-guide.md

Backend auth contract changed as follows:
1. POST /api/v1/auth/register
   - Does NOT return JWT anymore.
   - Returns:
     {
       "message": "Registration successful. Please check your email to verify your account.",
       "email": "user@example.com",
       "emailVerified": false
     }
2. GET /api/v1/auth/verify-email?token={token}
   - Returns:
     {
       "message": "Email verified successfully. You can now log in.",
       "email": "user@example.com",
       "emailVerified": true
     }
3. POST /api/v1/auth/resend-verification
   - Request:
     {
       "email": "user@example.com"
     }
   - Returns:
     {
       "message": "Verification email sent. Please check your inbox.",
       "email": "user@example.com",
       "emailVerified": false
     }
4. POST /api/v1/auth/login
   - Still returns JWT AuthResponse when verified.
   - May return 409 when email is not verified.

Implementation requirements:

A) Discover and map current auth files
1. Locate and list exact file paths for:
   - auth API service
   - auth repository
   - auth provider
   - auth models
   - auth screens
   - ApiClient/interceptor/header injection
   - auth gate/startup token restore logic
2. Summarize the current register and login flow assumptions.

B) Update API/data models
1. Add models:
   - RegisterResponseModel
   - VerifyEmailResponseModel
   - ResendVerificationRequestModel (or direct DTO usage if style fits codebase)
2. Keep existing AuthResponseModel for login.
3. Ensure robust JSON parsing and null safety.

C) Update service layer
1. In auth API service, implement methods:
   - register(email, password, name) -> RegisterResponseModel
   - verifyEmail(token) -> VerifyEmailResponseModel
   - resendVerification(email) -> VerifyEmailResponseModel
   - login(email, password) -> AuthResponseModel
2. Map HTTP status codes into typed/domain-friendly errors.

D) Update repository/provider logic
1. Remove any assumption that register returns JWT.
2. Do not persist token on register.
3. Persist token only after successful login.
4. Add provider state/actions for:
   - register success waiting-for-verification
   - verify email action
   - resend verification action
   - unverified login error state (409)

E) Update UI flow and screens
1. Register screen:
   - On success, show message and route to Verify Email screen.
2. Add Verify Email screen:
   - token input field (fallback if deep link not available)
   - Verify button
   - Resend verification button
3. Login screen:
   - Handle 409 with message: Email not verified.
   - Show CTA to go to Verify Email screen with prefilled email if possible.
4. Loading and error UX:
   - disable submit buttons while requests run
   - show inline error and success feedback

F) Deep link support for verification
1. Reuse existing app_links setup if present.
2. Handle links like:
   - http://localhost:8080/api/v1/auth/verify-email?token=...
   - https://sharecart.app/verify-email?token=... (if configured)
3. If token is detected via deep link, auto-fill/auto-trigger verification.

G) Error handling rules
1. 400: invalid input/token
2. 401: invalid credentials
3. 403: forbidden
4. 409: unverified email or business conflict
5. 500: generic backend failure

H) Update tests
1. Add/adjust unit tests for auth service/repository parsing and status mapping.
2. Add/adjust provider tests for new states and transitions.
3. Add widget tests for register -> verify -> login paths.

I) Documentation updates
1. Update any Flutter-side auth docs/readme in this repo to match new flow.
2. Keep endpoint references consistent with backend docs.

J) Delivery format
1. First provide a short implementation plan with exact files to edit.
2. Then apply the code changes.
3. Finally provide:
   - summary of changed files
   - behavior changes
   - any migration notes
   - commands/tests run and outcomes

Constraints:
- Preserve existing app architecture and coding style.
- Do not refactor unrelated modules.
- Keep backwards compatibility only where necessary and call it out.
- Prefer small focused changes over broad rewrites.
