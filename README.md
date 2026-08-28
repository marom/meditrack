# MediTrack

A clinic appointment & billing API built with Spring Boot.

It began as a deliberately messy course starter; the refactor described in
`CLAUDE.md` is complete. The code now follows a conventional
controller → service → repository layering with DTOs, centralised exception
handling, Bean Validation, status/payment enums, and a unit + slice + integration
test suite.

## Stack

- Java 21, Spring Boot 4.1, Maven
- MariaDB (schema owned by `db/meditrack_schema.sql`; `ddl-auto=validate`)
- springdoc-openapi for API docs

No Maven wrapper is bundled — use a system `mvn`.

## Run it

1. Create the database and seed data:
   ```
   mariadb -u root -p < db/meditrack_schema.sql
   ```
   This drops/recreates `meditrack_db` and seeds specialties, doctors and
   patients. Appointments, appointment services and payments are created only
   through the API.
2. Start the app (defaults connect to `jdbc:mariadb://localhost:3306/meditrack_db`
   as `root` with an empty password):
   ```
   mvn spring-boot:run
   ```
   Override the connection with env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
3. Or run everything in containers:
   ```
   docker compose up --build
   ```
   (MariaDB is published on host port 3307; the app on 8080.)

## API

Base path: `/api/v1`. Interactive docs once the app is running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

| Method & path | Purpose |
|---|---|
| `GET  /api/v1/specialties` | list specialties |
| `POST /api/v1/specialties` | create a specialty |
| `GET  /api/v1/doctors` | list doctors |
| `GET  /api/v1/doctors/{id}` | get one doctor |
| `POST /api/v1/doctors` | create a doctor |
| `GET  /api/v1/doctors/{id}/average-rating` | mean feedback rating for a doctor |
| `GET  /api/v1/patients` | list patients |
| `POST /api/v1/patients` | register a patient |
| `GET  /api/v1/appointments` | list appointments |
| `GET  /api/v1/appointments/{id}` | get one appointment |
| `POST /api/v1/appointments/book` | book an appointment |
| `POST /api/v1/appointments/{id}/cancel` | cancel (releases the slot, refunds the payment) |
| `POST /api/v1/appointments/{id}/complete` | mark completed (settles the payment) |
| `POST /api/v1/feedback` | submit feedback for a completed appointment |

### Smoke test

```
GET  http://localhost:8080/api/v1/doctors
GET  http://localhost:8080/api/v1/patients
POST http://localhost:8080/api/v1/appointments/book     {"patientId":1,"doctorId":1,"scheduledDate":"2026-09-01"}
POST http://localhost:8080/api/v1/appointments/1/cancel
```

### Errors

All errors return a consistent JSON body (`timestamp`, `status`, `error`,
`message`, `path`), handled centrally in `GlobalExceptionHandler`:

| Situation | Status |
|---|---|
| Unknown id | 404 |
| Business-rule violation (e.g. no slots, illegal status transition) | 400 |
| Bean Validation failure | 400, with a `fieldErrors` map |
| Duplicate resource (patient email, doctor licence, specialty name/slug) | 409 |

## Tests

```
mvn test      # unit (Mockito) + @WebMvcTest slice tests
mvn verify    # also runs the *IT integration tests
```

The `*IT` tests spin up a real MariaDB via Testcontainers (seeded from
`db/meditrack_schema.sql`), so `mvn verify` needs a running Docker/Podman daemon.
`SchemaValidationIT` boots the application against that schema to guard the
`ddl-auto=validate` contract.

## Layout

```
controller/  thin REST controllers, one per resource, @Valid on request bodies
service/     business logic; @Transactional; entity <-> DTO mapping
repo/        Spring Data JPA repositories
model/       JPA entities + AppointmentStatus / PaymentStatus enums
dto/         request/response DTOs (Lombok @Data / @Builder) + Bean Validation
exception/   GlobalExceptionHandler + ErrorResponse + typed exceptions
```
