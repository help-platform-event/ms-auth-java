# ms-auth-java

Auth and user service of the [H.E.L.P platform](https://github.com/help-platform-event).

It is a rewrite of the platform's original NestJS auth service, which it has fully replaced: 
the [`event-app`](https://github.com/help-platform-event/event-app) Gateway calls it over HTTP. It owns users, their profile and settings, and authentication (email/password, Google, JWT). It also publishes what happens to users as **Kafka events**, which other services (`ms-notification-java`) consume without ever calling it.

## What it does

- **Auth** (`/api/auth`): `signup`, `login`, `google` (OAuth2 code exchange, id_token verified), `refresh`, `logout`, `change-password`.
  - Access tokens are HS256 JWTs. The Gateway verifies them locally with the same secret.
  - Refresh tokens are opaque random values, stored as SHA-256 hashes, rotated on every refresh. Several sessions (devices) per user are allowed.
  - Passwords are hashed with Argon2.
- **Current user** (`/api/me`): `profile`, `availability`, `notifications`, each with GET and PATCH.
- **Users** (`/api/users`): `GET /{id}`, `GET /profiles?ids=…`.
- **Admin seed**: at startup, an ADMIN is created from `ADMIN_EMAIL`/`ADMIN_PASSWORD` if none exists (mandatory in prod, optional in dev).

Code layout: strict Clean Architecture (`domain` / `application` / `infrastructure` / `web`). The domain model has no framework annotations, and JPA entities live apart from it.

## Kafka events

Every event is published **after the database transaction commits**. It is keyed by user id, so one user's events stay in order.

| Topic | When |
|---|---|
| `auth.user.registered` | Signup, first Google login, admin seed |
| `auth.user.settings-changed` | Availability or notification settings updated |
| `auth.password.changed` | Password changed |

Only events that a consumer uses are published (today: `ms-notification-java`). The topics are declared at startup with 3 partitions (`KafkaTopicsConfig`): the service that publishes a topic is the one that declares it. If Kafka is down, a send gives up after 5 s instead of blocking the request for a minute.

## Run

To run it with the rest of the platform (Gateway, Front, notifications), see the [organization page](https://github.com/help-platform-event).

### Alone, on the host (hot reload)

```bash
./mvnw spring-boot:run
```

`spring-boot-docker-compose` starts this repo's `compose.yaml` (MySQL, Kafka, Kafka UI, Adminer), waits for it to be healthy, then Flyway migrates the database. The `app` service (this service in a container) is behind the `app` profile and only runs from event-app's stack, or with `docker compose --profile app up --build`.

For Google sign-in, copy `.env.example` to `.env` and fill in the OAuth client id and secret.

| | URL |
|---|---|
| API | http://localhost:8080 |
| Health | http://localhost:8080/actuator/health |
| Kafka UI (topics, messages, consumer groups) | http://localhost:8082 |
| Adminer (server `mysql`, user/password `ms_auth`) | http://localhost:8081 |
| MySQL | `localhost:3307` |
| Kafka (host clients) | `localhost:9094` (containers: `kafka:29092`) |

## Tests

```bash
./mvnw test
```

Docker must be running: the integration tests use Testcontainers (real MySQL and Kafka). They drive a full signup → login → refresh → logout flow over HTTP, and check the rows and the Kafka messages it produces.
