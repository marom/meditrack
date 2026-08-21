---
description: Flag any endpoint or log exposing patient email/phone
---

Scan this project for places that expose `Patient.email` or `Patient.phone`
(or any DTO/entity field carrying them) outside of what's strictly needed.

Check:
- `@RestController` methods returning `Patient` entities or DTOs directly —
  confirm `email`/`phone` are actually needed by the endpoint's purpose, not
  just carried along because the entity/DTO includes them.
- Nested associations that pull a `Patient` (or its DTO) into an unrelated
  response, e.g. an `Appointment`/`Doctor` response embedding the full patient.
- `log.info`/`log.debug`/`log.warn`/`log.error`/`System.out` calls that
  interpolate a `Patient` object, or its `email`/`phone` fields, directly.
- Exception messages or stack traces that include `email`/`phone`.

Report each finding as: file, line number, snippet, and why it's an exposure
(over-broad response vs. logged PII). Do not modify code — report only.
