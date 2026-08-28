# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

MediTrack is a Spring Boot clinic appointment & billing API. It is an **assignment starter that is intentionally written badly** — it runs, but violates most standard Spring Boot conventions on purpose (see "Known smells" below). The assignment is to use Claude Code to refactor it into clean code and then extend it. **Do not silently "clean up" the smells as a side effect of unrelated work** — the messy state is the starting point of a deliberate exercise; fix things when asked to, following the assignment brief.

## Commands

No Maven wrapper is bundled (adding one via `mvn -N wrapper:wrapper` is itself one of the assignment's refactor tasks). Use a system `mvn`.

- Run the app: `mvn spring-boot:run`
- Compile: `mvn compile`
- Package: `mvn package`
- Tests: none exist yet, and `pom.xml` deliberately omits `spring-boot-starter-test` — adding a test setup is part of the assignment, not a prerequisite for it.

# ecommerce-api

## Stack
- Java 21
- Spring Boot 4.1.0
- Maven
- MariaDB

## Package Structure
Base package: `com.marom.ecommerce`

Sub-packages:
- `controller`
- `service`
- `repository`
- `entity`
- `dto`
- `exception`
- `config`

## Architecture Rules
- Always use DTOs in controllers — never expose entities directly.
- All business logic lives in the service layer — keep controllers thin.
- Centralised exception handling via `GlobalExceptionHandler`.
- All endpoints prefixed with `/api/v1/`.

## Lombok
- `@RequiredArgsConstructor` for injection everywhere (with `final` fields). No `@Autowired`.
- Entities: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`.
- DTOs: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.

## Exceptions
All handled in `GlobalExceptionHandler`, returning `ErrorResponse` JSON:

| Exception                    | HTTP Status |
|------------------------------|-------------|
| `ResourceNotFoundException`  | 404         |
| `BusinessRuleException`      | 422         |
| `DuplicateResourceException` | 409         |
| `AccessDeniedException`      | 403         |

## Database
- MariaDB, database `ecommerce_db`.
- `ddl-auto=validate` — tables come from `schema.sql`, not Hibernate.


### Database

MariaDB, not embedded/in-memory. Before running the app, execute `db/meditrack_schema.sql` (e.g. `mariadb -u root -p < db/meditrack_schema.sql`). It drops and recreates `meditrack_db` from scratch and seeds `specialties`, `doctors`, and `patients`; appointments/services/payments are created only through the API. Connection settings (including a plaintext password) are in `src/main/resources/application.properties`; `spring.jpa.hibernate.ddl-auto=update` lets Hibernate mutate the schema at boot — one of the intentional smells.

### Manual smoke test

```
GET  /api/v1/doctors
GET  /api/v1/patients
POST /api/v1/appointments/book   {"patientId":1,"doctorId":1,"scheduledDate":"2026-08-01"}
POST /api/v1/appointments/1/cancel
```

## Architecture

Package root: `com.marom.meditrack`.

- `controller/` — `AppointmentController`, `CatalogController` (specialties + doctors, two resources in one controller), `PatientController`. **There is no service layer** — all business logic (booking rules, slot-capacity checks, appointment-number generation, status transitions) lives directly in the controllers, which also inject repositories with field-level `@Autowired` and return JPA entities straight out as HTTP responses (no DTOs).
- `model/` — JPA entities (`Appointment`, `AppointmentService`, `Doctor`, `Patient`, `Payment`, `Specialty`), all annotated `@Data @Entity`. Associations (`Doctor.specialty`, `Appointment.patient/doctor/services`, `Payment.appointment`) are all `FetchType.EAGER`.
- `repo/` — plain `JpaRepository<T, Long>` interfaces, one per entity, essentially no custom query methods beyond `AppointmentRepository.countByDoctorAndScheduledDate`.

Data model (see `db/meditrack_schema.sql` for the authoritative schema and FK relationships): `specialties` → `doctors` → `appointments` → `appointment_services`, with a 1:1 `payments` row per appointment. `doctors.daily_slot_capacity` is the per-day booking limit enforced (badly) in `AppointmentController.book`. `appointments.status` is a free-text column with no enum or transition rules — `cancel` just sets the string `"CANCELLED"` and does **not** release the doctor's slot or refund the payment.

## Known smells (intentional — this is the assignment's checklist, not a bug list)

- Money stored as `double` (entities and DTOs alike) instead of `BigDecimal`.
- `@Data` on JPA entities instead of `@Getter @Setter`.
- No service layer — controllers hold all business logic.
- No exception handling — missing rows return `null` instead of 404s; failures throw generic `RuntimeException` → HTTP 500.
- Field injection via `@Autowired` instead of constructor injection.
- `ddl-auto=update` and a plaintext DB password in `application.properties`.
- `appointment.status` is unvalidated free text.
- Cancelling an appointment doesn't restore the doctor's daily slot or refund the `payments` row.
- No DTOs (entities serialized directly, including JPA associations), no request validation, no API docs, no tests.

## Target architecture (refactor requirements)

These are the required end states for MediTrack. They describe what the code must look like **after** a smell is fixed — not the current state (see "Known smells" above) — and **do not** license fixing them proactively as a side effect of unrelated work; only apply them when the task at hand calls for it.

- **Dependency injection**: constructor injection via `@RequiredArgsConstructor` with `final` fields, everywhere. Never `@Autowired` (field or setter injection).
- **Money**: `BigDecimal` for every money-related field, in entities, DTOs, and calculations alike. Never `double`/`float`.
- **DTOs**: controllers accept and return DTOs only. JPA entities (and their associations) must never be exposed directly as request/response bodies.
- **Controllers are thin**: booking rules, slot-capacity checks, appointment-number generation, status transitions, and all other business logic live in a service layer, not in controllers.
- **API prefix**: every endpoint lives under `/api/v1/`.
- **Exception handling**: centralised via a `GlobalExceptionHandler`, returning a consistent `ErrorResponse` JSON body for all error cases (no bare `null` returns, no unhandled `RuntimeException` → 500s).
- **Schema management**: `spring.jpa.hibernate.ddl-auto=validate`, with `db/meditrack_schema.sql` as the single source of truth for table structure — Hibernate must not be allowed to mutate the schema at boot.

The user's other global Java conventions (`@Getter @Setter @Builder` on entities vs `@Data`/`@Builder` on DTOs, AssertJ-based tests) also describe target state to refactor toward.
