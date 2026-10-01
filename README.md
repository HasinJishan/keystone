# KEYSTONE — Field Service Management Platform

Work-order platform for Meridian Facilities Management: dispatchers raise and assign jobs,
technicians update them from the field, managers watch SLAs, customers track requests.

**Stack:** Spring Boot 3.3 (Java 21) · Spring Security + JWT · Spring Data JPA · PostgreSQL · Flyway · React + TypeScript (Vite) · springdoc-openapi

## Architecture
`controller` (thin REST) → `service` (business rules, lifecycle state machine, SLA) → `repository` (Spring Data JPA) → PostgreSQL.
DTOs at the boundary, `@ControllerAdvice` for uniform errors, `@PreAuthorize` role checks on the server.
Schema is managed only by Flyway scripts in `backend/src/main/resources/db/migration`.

## Authentication module
Register (always a CUSTOMER) · Login (JWT, BCrypt) · Forgot/Reset password (emailed single-use token) ·
**Logout** (`POST /api/auth/logout` adds the token to an in-memory blocklist; `JwtAuthFilter` then rejects it with 401).

## Run locally
1. Create an empty database: `createdb keystone` (user/password `keystone`), or `docker compose up -d`.
2. Backend: `cd backend && ./mvnw spring-boot:run` (or `mvn spring-boot:run`) → http://localhost:8080
   - On first start Flyway runs `V1__init_schema.sql`, then `DataSeeder` inserts demo data.
3. Frontend: `cd frontend && cp .env.example .env && npm install && npm run dev` → http://localhost:5173
4. API docs: http://localhost:8080/swagger-ui.html

## Environment variables (backend)
| Variable | Purpose | Default (dev only) |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | localhost / keystone |
| `JWT_SECRET` | JWT signing key (**set a long random value in production**) | dev placeholder |
| `JWT_EXPIRATION_MS` | Token lifetime | 86400000 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM` | SMTP for reset emails (use a **dummy** project mailbox, port 587) | localhost:1025 |
| `FRONTEND_URL`, `ALLOWED_ORIGINS` | Reset-link base URL and CORS | http://localhost:5173 |

Frontend: `VITE_API_URL` = deployed API base URL.

## Migrations & seed
- Migrations run automatically at startup (`spring.flyway.enabled=true`); Hibernate only **validates** (`ddl-auto=validate`).
- To add a change, create `V2__description.sql` — never edit an applied migration.
- An existing database created earlier by Hibernate is adopted automatically (`baseline-on-migrate`).
- Seed data is inserted once by `DataSeeder` (skipped if users exist).

## Seed logins (password for all: `Passw0rd!`)
| Role | Email |
|---|---|
| Admin | admin@keystone.dev |
| Manager | manager@keystone.dev |
| Dispatcher | dispatcher@keystone.dev |
| Technician | technician@keystone.dev |
| Customer | customer@keystone.dev |

## Tests
`cd backend && mvn test`

## Live URLs
- API: _add link_ · Swagger: _add link_/swagger-ui.html · Frontend: _add link_ · Demo video: _add link_
