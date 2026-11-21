# LeaveFlow

![CI](https://github.com/lafleur06/leaveflow/actions/workflows/ci.yml/badge.svg)

An HR leave management REST API built with Java 21 and Spring Boot. Employees request leave, managers approve or reject it for their own team, and HR manages staff and entitlements. Annual leave entitlement follows the Turkish Labour Law (Law No. 4857, Art. 53).

> **Status:** Phase 1 – departments and employees. See the [roadmap](#roadmap).

## Tech stack

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 4.2, Maven (wrapper) |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL 17, Flyway migrations |
| API | Spring Web MVC, Bean Validation, springdoc-openapi (Swagger UI) |
| Ops | Spring Boot Actuator, Docker Compose |
| Testing | JUnit 5, Testcontainers |
| CI | GitHub Actions |

## Getting started

### Prerequisites

- JDK 21
- Docker Desktop (running)

### Run the application

```bash
./mvnw spring-boot:run
```

Spring Boot's Docker Compose support starts PostgreSQL from `compose.yaml` and wires the datasource automatically. You do not need to install a database.

- Swagger UI (try every endpoint from the browser): <http://localhost:8080/swagger-ui.html>
- OpenAPI document: <http://localhost:8080/v3/api-docs>
- Health check: <http://localhost:8080/actuator/health>

### Run the tests

```bash
./mvnw verify
```

Integration tests start a throwaway PostgreSQL container through Testcontainers. On machines without Docker they are skipped, not failed. CI always runs them.

## API

Interactive documentation is available at `/swagger-ui.html` while the application is running.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/departments` | List departments (paged, sorted by name) |
| `GET` | `/api/departments/{id}` | Get one department |
| `POST` | `/api/departments` | Create a department |
| `PUT` | `/api/departments/{id}` | Replace a department |
| `DELETE` | `/api/departments/{id}` | Delete a department (only when it has no employees) |
| `GET` | `/api/employees` | Search employees: `departmentId`, `active`, `q` (name or email), plus `page`, `size`, `sort` |
| `GET` | `/api/employees/{id}` | Get one employee |
| `POST` | `/api/employees` | Create an employee |
| `PUT` | `/api/employees/{id}` | Replace an employee |
| `DELETE` | `/api/employees/{id}` | Deactivate an employee (soft delete) |

Example:

```bash
curl -X POST localhost:8080/api/employees -H "Content-Type: application/json" -d '{
  "firstName": "Ayse", "lastName": "Yilmaz", "email": "ayse.yilmaz@example.com",
  "jobTitle": "Backend Developer", "birthDate": "1998-07-11", "hireDate": "2024-09-02",
  "departmentId": 1, "managerId": 1
}'
```

Paged responses look like `{ "content": [ ... ], "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 } }`.

### Business rules

- Department names and employee emails are unique, case-insensitively (enforced in the service **and** by unique indexes).
- An employee must be at least 15 on the hire date (Labour Law No. 4857, Art. 71).
- An employee cannot manage themselves, an inactive employee cannot be a manager, and reporting cycles (A → B → A) are rejected.
- Employees are never hard-deleted, because their leave history must survive. Deactivation is blocked while they still manage active staff.
- A department can only be deleted when it has no employees.

### Errors

Every error is an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json` response:

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "One or more fields are invalid",
  "errors": { "email": "must be a well-formed email address" }
}
```

| Status | When |
|---|---|
| 400 | Invalid body, malformed JSON, wrong parameter type, unknown sort property |
| 404 | Resource not found |
| 409 | Duplicate name/email, deleting a department that has employees, deactivating a manager with active reports |
| 422 | Valid request that breaks a business rule (minimum age, reporting cycle, inactive manager) |

Validation messages are in English by default; send `Accept-Language: tr` to get them in Turkish.

## Design decisions

- **Feature-based packages** (`department`, `employee`, `common`) instead of layer-based ones, so each feature's controller, service, repository and DTOs sit together.
- **DTOs as Java records**, mapped with small static `from(...)` methods. Entities never leave the service layer, and `spring.jpa.open-in-view` is off, so lazy loading cannot leak into the web layer.
- **Flyway owns the schema**; Hibernate only validates it (`ddl-auto: validate`).
- **N+1 avoided** on the employee list with an `@EntityGraph` that fetches department and manager in the same query.
- **Search built with JPA Specifications**, so optional filters combine into one query. User input is escaped before it is used in `LIKE`.
- **Integration tests run against real PostgreSQL** (Testcontainers), not an in-memory database, so unique indexes and SQL behave exactly as in production.

## Project structure

```
src/main/java/com/batuhanturnay/leaveflow
├── common       base entity, auditing, global error handling
├── department   department API
└── employee     employee API
src/main/resources/db/migration   Flyway migrations
src/test/java/...                 API integration tests (MockMvc + Testcontainers)
compose.yaml                      local PostgreSQL
.github/workflows/ci.yml          build + test on every push / PR
```

## Roadmap

- [x] **Phase 0 – Skeleton:** Maven project, PostgreSQL via Docker Compose, Flyway, Actuator, CI
- [x] **Phase 1 – Core domain:** Employee and Department, DTOs, validation, global error handling (`ProblemDetail`), pagination
- [ ] **Phase 2 – Leave workflow:** leave request state machine, entitlement calculation, overlap check, working-day calculation
- [ ] **Phase 3 – Security:** JWT authentication, roles (EMPLOYEE / MANAGER / HR), team-scoped approvals
- [ ] **Phase 4 – Testing:** unit tests for business rules, integration tests with Testcontainers, coverage report
- [ ] **Phase 5 – Extras:** scheduled yearly accrual, reporting endpoints, CSV/Excel export, auditing
- [ ] **Phase 6 – Showcase:** ~~OpenAPI docs~~ (done early: Swagger UI), ER diagram, design decisions, live demo
