# KEYSTONE — Field Service Management Platform

Work-order platform for **Meridian Facilities Management**. Dispatchers raise and assign jobs,
technicians update them from the field, managers watch SLAs and dashboards, and customers
raise requests and track progress.

Built for the Zidio Development Java Full-Stack Engineering Project (Project KEYSTONE).

## Live demo

| | Link |
|---|---|
| Frontend | https://keystone-theta-six.vercel.app |
| API | https://keystone-nbv7.onrender.com |
| Swagger UI | https://keystone-nbv7.onrender.com/swagger-ui.html |
| Demo video | _add unlisted link_ |

> The API runs on a free Render instance, which sleeps when idle. The first request after a
> break can take up to ~50 seconds to respond.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Back end | Spring Boot 3.3, Spring Web, Bean Validation |
| Security | Spring Security, stateless JWT, BCrypt |
| Persistence | Spring Data JPA / Hibernate, PostgreSQL |
| Migrations | Flyway |
| API docs | springdoc-openapi (Swagger UI) |
| Front end | React + TypeScript (Vite) |
| Deployment | Render (API, Docker), Neon (PostgreSQL), Vercel (front end) |

## Architecture

```
React SPA  ->  Controllers  ->  Services  ->  Repositories  ->  PostgreSQL (Flyway)
                (thin REST)     (rules, state    (Spring Data
                                 machine, SLA)    JPA)
```

- **Controllers:** HTTP only (validate input, map DTOs, delegate to services).
- **Services:** business rules, the work-order lifecycle state machine, SLA logic, transactions.
- **Repositories:** Spring Data JPA; queries are scoped so customers only read their own data.
- **DTOs** at the boundary (entities are never returned to the client).
- **`@ControllerAdvice`** returns one consistent JSON error shape (never a stack trace).
- **Server-side authorisation** with `@PreAuthorize` on every protected action.
- **Schema** is managed only by Flyway scripts in `backend/src/main/resources/db/migration`.

### Project structure

```
keystone/
  backend/                      Spring Boot service
    src/main/java/.../controller    REST controllers
    src/main/java/.../service       business logic + state machine
    src/main/java/.../repository    Spring Data JPA
    src/main/java/.../domain        JPA entities
    src/main/java/.../dto           request/response objects
    src/main/java/.../security      JWT, filter, config, token blocklist
    src/main/java/.../exception     global error handling
    src/main/resources/db/migration Flyway scripts
    src/test/                       tests
  frontend/                     React + TypeScript (Vite)
  docker-compose.yml            local PostgreSQL
  README.md
```

## Authentication module (Module 1)

| Feature | How it works |
|---|---|
| Register | Public sign-up. Always creates a **CUSTOMER** (there is no role field, so nobody can self-register as an internal role). Redirects to the login page. |
| Login | Email + password. Returns a signed JWT (carries email, name, role) that expires after 24 hours. |
| Passwords | Stored only as **BCrypt** hashes. |
| Forgot password | Generates a random, single-use token that expires in 30 minutes and emails a reset link. The response is identical whether or not the email exists (no account probing). |
| Reset password | Validates the token and expiry, sets the new password, then clears the token. |
| Logout | `POST /api/auth/logout` adds the token to an in-memory blocklist (`TokenKillService`, a `ConcurrentHashMap`-backed set). `JwtAuthFilter` rejects any blocklisted token with **401**, even before it expires. The blocklist is cleared when the server restarts. |
| Roles | `ADMIN`, `MANAGER`, `DISPATCHER`, `TECHNICIAN`, `CUSTOMER`. Roles are enforced on the server with Spring Security method-level authorisation. |

### Auth endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a customer account |
| POST | `/api/auth/login` | Authenticate, returns JWT + role |
| POST | `/api/auth/forgot-password` | Email a reset link |
| POST | `/api/auth/reset-password` | Set a new password with the token |
| POST | `/api/auth/logout` | Kill the current token |

All other endpoints require `Authorization: Bearer <token>`. Full reference: Swagger UI.

## Run locally

**Prerequisites:** Java 21, Maven, Node 18+, Docker Desktop.

1. **Database:** from the project root run `docker compose up -d`
   (PostgreSQL 16 on port 5432; database, user and password are all `keystone`).
2. **Back end:**
   ```
   cd backend
   mvn spring-boot:run
   ```
   API at http://localhost:8080. On first start Flyway creates the schema, then `DataSeeder` inserts demo data.
3. **Front end:**
   ```
   cd frontend
   cp .env.example .env
   npm install
   npm run dev
   ```
   App at http://localhost:5173.
4. **API docs:** http://localhost:8080/swagger-ui.html

To reset the local database and test a fresh install: `docker compose down -v` then `docker compose up -d`.

## Environment variables (back end)

No secrets are committed. Values come from the environment; the defaults below are for local development only.

| Variable | Purpose | Default (dev only) |
|---|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host/db?sslmode=require` | `jdbc:postgresql://localhost:5432/keystone` |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials | `keystone` / `keystone` |
| `JWT_SECRET` | JWT signing key (**set a long random value in production**) | dev placeholder |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24 h) |
| `MAIL_HOST`, `MAIL_PORT` | SMTP server (Gmail: `smtp.gmail.com`, `587`) | `localhost`, `1025` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP login (use a **dummy project mailbox** and an app password, never a personal account) | empty |
| `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Set both to `true` for Gmail | `false` |
| `BREVO_API_KEY` | If set, reset emails are sent over **HTTPS via the Brevo API** instead of SMTP (needed on Render's free tier, which blocks SMTP ports) | empty |
| `MAIL_FROM` | Sender address (with Brevo it must be a verified sender) | `noreply@keystone.dev` |
| `FRONTEND_URL` | Base URL used in reset-password links | `http://localhost:5173` |
| `ALLOWED_ORIGINS` | CORS: allowed front-end origin(s) | `http://localhost:5173` |
| `SEED_ENABLED` | Create demo accounts on first start | `true` |
| `SEED_PASSWORD` | Password for the demo accounts | `Passw0rd!` |

Front end: `VITE_API_URL` = base URL of the API (see `frontend/.env.example`).

### Email on the live site
Render's free tier blocks outbound SMTP (ports 25/465/587), so the deployed API sends reset emails through the
Brevo HTTPS API: set `BREVO_API_KEY` and set `MAIL_FROM` to a sender verified in Brevo. Locally, SMTP (MailHog or a
Gmail app password) still works when `BREVO_API_KEY` is empty.

### Testing the reset email locally
Locally there is no mail server by default, so the email is not delivered (the app still returns its normal message).
Either run a mail catcher such as MailHog (SMTP `localhost:1025`), set the `MAIL_*` variables for a dummy Gmail account,
or read the token from the database:
```
docker exec -it keystone-db psql -U keystone -d keystone -c "select email, reset_token from users where reset_token is not null;"
```
and open `http://localhost:5173/reset-password?token=<token>`.

## Database migrations and seed data

- Flyway runs automatically at startup. Hibernate only **validates** the schema (`ddl-auto=validate`); it never changes it.
- `V1__init_schema.sql` creates all tables with foreign keys, unique constraints, indexes and CHECK rules
  (for example, stock can never go negative).
- To change the schema, add a new file such as `V2__add_column.sql`. **Never edit an applied migration.**
- A database previously created by Hibernate is adopted automatically (`baseline-on-migrate`).
- `DataSeeder` inserts demo customers, sites, users, parts and work orders **once** (skipped if users already exist).
  Passwords are BCrypt-hashed in code, which is why users are seeded in Java rather than SQL.

## Seed logins (for reviewers)

These accounts are not shown anywhere in the app UI.

| Role | Email |
|---|---|
| Admin | `admin@keystone.dev` |
| Manager | `manager@keystone.dev` |
| Dispatcher | `dispatcher@keystone.dev` |
| Technician | `technician@keystone.dev` |
| Customer | `customer@keystone.dev` |

- **Local:** password is `Passw0rd!` (the `SEED_PASSWORD` default).
- **Live site:** the password is provided in the submission form.

## Tests

```
cd backend
mvn test
```

Tests run against an in-memory H2 database (profile `test`) and cover:
login (valid and wrong password), missing and tampered tokens, logout (a logged-out token returns 401),
a customer being blocked from another customer's work order, a technician being blocked from closing a job,
a customer being blocked from deleting, and an illegal lifecycle jump (NEW to COMPLETED) returning 409.

## Deployment

| Part | Service | Notes |
|---|---|---|
| API | Render (Docker, `backend/Dockerfile`) | Set the environment variables above in the Render dashboard |
| Database | Neon (PostgreSQL) | Flyway builds the schema on the first start of an empty database |
| Front end | Vercel | Set `VITE_API_URL` to the API URL; `ALLOWED_ORIGINS` on the API must match the Vercel URL |

Deploying onto a database that is not empty and was not created by Flyway can fail schema validation.
For a clean start, empty it first (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;`) and redeploy.

## Security notes

- Passwords: BCrypt only. JWTs are signed, expire, and are rejected after logout.
- Authorisation is enforced on the server for every protected endpoint (the UI hiding a button is not a security control).
- Secrets (database credentials, JWT secret, mail password) come from the environment and are never committed.
- Registration cannot create internal roles; forgot-password never reveals whether an account exists.