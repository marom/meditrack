# API Design Rules
- All endpoints: `/api/v1/resource`.

## HTTP Methods & Status Codes
| Operation | Verb   | Success Response                  |
|-----------|--------|-----------------------------------|
| Create    | POST   | `201 Created` with body           |
| Retrieve  | GET    | `200 OK`                          |
| Update    | PUT    | `200 OK` with updated body        |
| Delete    | DELETE | `204 No Content`                  |

## Error Responses
- Validation errors → `400 Bad Request`, listing all field errors.
- Business rule violations → `400 Bad Request`.
- Duplicate resource → `409 Conflict`.
