# PulseOps Lite

A backend service for monitoring HTTP services and tracking incidents, built with Spring Boot. It watches endpoints on a schedule, records their status history, flags outages when a check fails repeatedly, and exposes a public status page API for incidents.

## Status

Working now:

- User registration and login with JWT access tokens
- Stateless security using Spring's OAuth2 Resource Server, with roles mapped to authorities
- PostgreSQL schema managed by Flyway migrations
- Health endpoint via Spring Boot Actuator
- Database schema and JPA entities for monitored services and their HTTP monitors

In progress:

- CRUD endpoints for managing services and monitors (admin only)

Planned:

- Scheduled checks every 60 seconds and status calculation
- Paginated check history
- Manual incident management with a timeline
- Public status and incident endpoints
- OpenAPI docs and a GitHub Actions pipeline

## The problem it solves

If you run a few services, you want to know when one goes down before your users tell you. PulseOps covers that loop: register a service, attach one or more HTTP checks to it, and let a scheduler poll them. When a check fails several times in a row the service is marked degraded, and operators can open an incident and post updates that show up on a public status page.

The scope is intentionally focused on the backend. There is no frontend, message queue, or Kubernetes layer. What it does have is authentication, scheduled work, a versioned relational schema, and a tested REST API.

## Tech stack

- Java 21, Spring Boot 4.1.0
- Spring Web MVC for the REST API
- Spring Data JPA and Hibernate over PostgreSQL 16
- Flyway for versioned database migrations
- Spring Security with OAuth2 Resource Server for stateless JWT auth (HS256)
- Jakarta Bean Validation for request validation
- Spring Boot Actuator for health checks
- JUnit 5, Mockito, and MockMvc for tests
- Docker Compose for the local database
- Maven as the build tool

## Architecture

The code is organised by feature, not by technical layer. Each feature owns its web, application, domain, and persistence classes:

```
com.mateja.pulseops
├── auth            registration, login, JWT issuing
├── monitoreservice the service being watched
├── httpmonitor     an HTTP check attached to a service
├── security        security config, JWT encoder/decoder, properties
└── common.web      shared error handling
```

Within a feature the layers are:

- `web` holds controllers and the request/response records
- `application` holds the service classes with the business logic
- `domain` holds the JPA entities and enums
- `persistence` holds the Spring Data repositories

Keeping features together means everything related to auth lives in one place, so you can read a slice of the app end to end without jumping across four sibling folders.

## Design decisions

A few choices are worth calling out.

**Stateless JWT, no sessions.** The API never creates an HTTP session. Every request carries a Bearer token that the server validates on its own using a shared secret. This keeps the service easy to scale horizontally and removes session storage from the picture. There are no refresh tokens yet; access tokens live for one hour.

**Roles live in the token, mapped to authorities on the way in.** The login response embeds a `roles` claim. A converter turns that claim into Spring authorities with a `ROLE_` prefix, so `hasRole("ADMIN")` works as expected. Getting this prefix wrong is a common Spring trap where authentication succeeds but every authorization check quietly fails, so it is handled explicitly.

**Login errors are intentionally vague.** A wrong password and an unknown email both return the same 401 with the same message. Telling the two apart would let someone probe which emails are registered.

**Flyway owns the schema, Hibernate only validates it.** Hibernate runs in `validate` mode, so it checks that the entities match the tables but never alters the database. All schema changes go through numbered SQL migrations. This makes the schema explicit and reviewable instead of something Hibernate guesses at startup.

**Passwords are hashed with BCrypt.** Raw passwords are never stored. The salt is embedded in each hash, so two users with the same password still get different stored values.

**Errors follow RFC 7807.** Failures come back as `application/problem+json` with a consistent shape, and validation errors include a per-field map so a client can show messages inline.

**The check config is separate from the check results.** An `HttpMonitor` describes what to poll and what to expect. The record of each individual poll will be a separate entity when the scheduler is built. Mixing the two would make the current status impossible to derive cleanly later.

## Data model

Two tables so far, in a one-to-many relationship.

- `monitored_service`: the thing being watched, for example "Payments API". Names are unique, case-insensitive.
- `http_monitor`: an HTTP check belonging to a service, with a target URL, method (GET or HEAD), expected status code, and an enabled flag. Deleting a service cascades to its monitors.

The `app_users` table from the auth milestone stores accounts with an email, a BCrypt hash, and a role.

## Running locally

You need a JDK (the build targets Java 21) and Docker for the database.

First, create a `.env` file in the project root. It is git-ignored and holds your local secrets:

```
POSTGRES_USER=postgres
POSTGRES_PASSWORD=change-me
POSTGRES_DB=PulseOps
POSTGRES_PORT=5433
JWT_SECRET=<base64-encoded random key, at least 32 bytes>
```

The JWT secret must be standard Base64 and decode to at least 256 bits. On Windows PowerShell you can generate one like this:

```powershell
$b = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

Then start the database and run the app:

```
docker compose up -d
./mvnw spring-boot:run
```

Spring Boot's Docker Compose support wires the datasource from the running container automatically, and Flyway applies the migrations on startup. The local database listens on port 5433 to avoid clashing with a native Postgres on the default 5432.

Run the tests with:

```
./mvnw clean verify
```

## API

The endpoints that exist today:

| Method | Path                 | Auth   | Description                          |
|--------|----------------------|--------|--------------------------------------|
| POST   | `/api/auth/register` | Public | Create an account, returns 201       |
| POST   | `/api/auth/login`    | Public | Exchange credentials for a JWT       |
| GET    | `/actuator/health`   | Public | Liveness check                       |

Everything else requires a valid Bearer token, and the service and monitor endpoints will require the admin role once they land.

## Testing

The auth flow is covered by unit tests on the service layer with Mockito and web-layer tests with MockMvc. Together they check the success paths plus the 400, 401, and 409 responses. The project does not use Testcontainers or hit a real database in tests by choice, to keep the suite fast and focused on the application logic.

## Roadmap

1. Foundation: Postgres, Flyway, Actuator, health check (done)
2. Authentication and authorization with JWT (done)
3. Service and HTTP monitor management (in progress)
4. Scheduled checks every 60 seconds
5. Status calculation: three consecutive failures mark a service degraded, one success restores it
6. Paginated check history
7. Manual incidents with a status timeline
8. Public status and incident endpoints
9. OpenAPI documentation and cleanup
10. GitHub Actions CI and a final security pass

## License

Released under the MIT License. See [LICENSE](LICENSE) for details.
