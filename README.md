# CyberSocial Backend

Production-ready Spring Boot 3.x backend for the CyberSocial frontend application.

## Prerequisites

- Java 21
- Docker, for local PostgreSQL
- Maven Wrapper is included, so a local Maven install is optional

## Tech Stack

- Spring Boot 3.x
- Spring Security 6
- Spring Data JPA
- Flyway
- PostgreSQL
- JWT with `jjwt`
- Lombok

## Start PostgreSQL

```bash
docker run -d \
-p 5432:5432 \
-e POSTGRES_PASSWORD=secret \
-e POSTGRES_USER=cs_user \
-e POSTGRES_DB=cybersocial \
postgres
```

PowerShell equivalent:

```powershell
docker run -d `
-p 5432:5432 `
-e POSTGRES_PASSWORD=secret `
-e POSTGRES_USER=cs_user `
-e POSTGRES_DB=cybersocial `
postgres
```

## Environment Variables

The app reads database and JWT configuration from environment variables. Defaults are provided for local development in `src/main/resources/application.yml`.

```env
DB_URL=jdbc:postgresql://localhost:5432/cybersocial
DB_USERNAME=cs_user
DB_PASSWORD=secret
JWT_SECRET=replace-with-at-least-64-random-characters-for-production-use
JWT_ISSUER=cybersocial
JWT_ACCESS_TOKEN_MINUTES=15
JWT_REFRESH_TOKEN_DAYS=30
SERVER_PORT=8080
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
CLOUDINARY_FOLDER=cybersocial/posts
UPLOAD_MAX_IMAGE_SIZE=5MB
```

For production, replace `JWT_SECRET`, database credentials, and the CORS production placeholder in `application.yml`.

## Run

From this directory:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Build:

```bash
./mvnw clean package
```

Flyway runs automatically on startup and applies `src/main/resources/db/migration/V1__init.sql`.

## JWT Flow

CyberSocial uses stateless access-token authentication.

1. `POST /api/auth/register` or `POST /api/auth/login` returns an access token in the JSON response.
2. The frontend sends the access token on protected requests as `Authorization: Bearer <accessToken>`.
3. Access tokens expire after 15 minutes.
4. `POST /api/auth/refresh` reads the refresh token from the `Refresh` cookie and returns a new access token.
5. `POST /api/auth/logout` revokes the current refresh token and expires the cookie.

## Refresh Token Cookie

Refresh tokens are stored only in an HttpOnly cookie:

- Cookie name: `Refresh`
- `HttpOnly=true`
- `Secure=true`
- `SameSite=Lax`
- Path: `/api/auth`
- Lifetime: 30 days

Because `Secure=true`, browsers only send the cookie over HTTPS. For local browser testing over plain HTTP, use an HTTPS dev proxy or temporarily relax the cookie setting in a local-only profile.

## Frontend Integration Notes

- Allowed CORS origins:
  - `http://localhost:5173`
  - `https://cybersocial.example.com`
- Send `credentials: "include"` on login, register, refresh, and logout requests so the refresh cookie is stored/sent.
- Store the access token in frontend memory when possible and attach it to `/api/**` requests with the Bearer header.
- Refresh-token rotation is enabled: every refresh revokes the previous refresh token and issues a new cookie.

## API Endpoint Summary

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Users and theme:

- `GET /api/users/me`
- `PUT /api/users/me`
- `GET /api/theme`
- `PUT /api/theme`

Posts:

- `GET /api/posts?page=0&size=20`
- `GET /api/posts/{id}`
- `POST /api/posts`
- `DELETE /api/posts/{id}`

Uploads:

- `POST /api/uploads/images` with `multipart/form-data` field `file`
- Uploaded images are stored in Cloudinary and the response returns the Cloudinary `secure_url`

Friends:

- `GET /api/friends`
- `GET /api/friends/requests/incoming`
- `GET /api/friends/requests/outgoing`
- `GET /api/friends/search?query=duy&page=0&size=20`
- `POST /api/friends/requests/{userId}`
- `POST /api/friends/requests/{requestId}/accept`
- `DELETE /api/friends/requests/{requestId}`
- `DELETE /api/friends/{friendshipId}`

Notifications:

- `GET /api/notifications?page=0&size=20`
- `PATCH /api/notifications/{id}/read`

AI analysis:

- `POST /api/ai/analyze`

Graph:

- `GET /api/graph/nodes`
- `GET /api/graph/edges`

Health:

- `GET /actuator/health`

## Security Rules

- `/api/auth/login`, `/api/auth/register`, and `/api/auth/refresh` are public.
- All other `/api/**` endpoints require a valid Bearer access token.
- Authentication is stateless; no HTTP session is created.
- Unauthorized and forbidden responses use the same standardized API envelope as application errors.

## Response Format

Successful responses:

```json
{
  "success": true,
  "message": "Success",
  "data": {},
  "timestamp": "2026-05-19T00:00:00Z"
}
```

Error responses:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "code": "VALIDATION_ERROR",
    "path": "/api/auth/register",
    "validationErrors": {
      "email": "Email must be valid"
    }
  },
  "timestamp": "2026-05-19T00:00:00Z"
}
```
