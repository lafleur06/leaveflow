# LeaveFlow

![CI](https://github.com/lafleur06/leaveflow/actions/workflows/ci.yml/badge.svg)

An HR leave management REST API built with Java 21 and Spring Boot. Employees request leave, managers approve or reject it for their own team, and HR manages staff and entitlements. Annual leave entitlement follows the Turkish Labour Law (Law No. 4857, Art. 53).

> **Status:** Phase 2 – leave requests, approvals and balances. See the [roadmap](#roadmap).

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
| `GET` | `/api/employees/{id}/leave-balance` | Annual leave balance: earned, used, pending, available days and the next accrual |
| `GET` | `/api/leave-requests` | Search: `employeeId`, `managerId` (a manager's team), `status`, `type`, plus paging |
| `GET` | `/api/leave-requests/{id}` | Get one leave request |
| `POST` | `/api/leave-requests` | Submit a leave request (`ANNUAL`, `SICK` or `UNPAID`) |
| `POST` | `/api/leave-requests/{id}/approve` | Approve (direct manager only) |
| `POST` | `/api/leave-requests/{id}/reject` | Reject with a note (direct manager only) |
| `POST` | `/api/leave-requests/{id}/cancel` | Cancel a pending request, or an approved one before it starts |
| `GET` | `/api/holidays?year=2026` | Public holidays of a year |
| `POST` | `/api/holidays` | Add a public holiday |
| `DELETE` | `/api/holidays/{id}` | Remove a public holiday |

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

**Leave**

- Leave requests follow a state machine: `PENDING → APPROVED | REJECTED | CANCELLED`, and `APPROVED → CANCELLED` only before the leave starts. Any other transition is a `409`.
- Leave is counted in **working days**: weekends and official public holidays are excluded (Labour Law Art. 56). The count is fixed when the request is created.
- Annual leave entitlement follows Labour Law Art. 53 and is earned on every hire-date anniversary:

  | Completed years of service | Days per year |
  |---|---|
  | 1 – 5 | 14 |
  | 6 – 14 | 20 |
  | 15 or more | 26 |

  Employees aged 18 or younger, or 50 or older, on the anniversary get at least 20 days. Nothing is earned during the first year.
- `available = earned − approved − pending`: pending requests reserve their days, so the same days cannot be booked twice.
- Annual and unpaid leave cannot start in the past; sick leave can (reports are often handed in afterwards).
- An employee cannot have two overlapping pending or approved requests.
- Only the employee's direct manager can approve or reject; a rejection needs a note. Employees without a manager wait for the HR role (Phase 3).

**Public holidays.** The fixed-date national holidays of 2026–2027 and the religious holidays of 2026 are seeded by Flyway. Religious holidays move every year, so HR adds the future ones through `POST /api/holidays` once they are announced. Half-day eves (arife, 28 October afternoon) are not modelled yet.

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
| 403 | Deciding on a leave request without being the employee's direct manager |
| 404 | Resource not found |
| 409 | Duplicate name/email/holiday, deleting a department that has employees, deactivating a manager with active reports, overlapping leave, invalid status transition, concurrent update |
| 422 | Valid request that breaks a business rule (minimum age, reporting cycle, insufficient balance, no working days in the period, leave in the past) |

Validation messages are in English by default; send `Accept-Language: tr` to get them in Turkish.

## Design decisions

- **Feature-based packages** (`department`, `employee`, `common`) instead of layer-based ones, so each feature's controller, service, repository and DTOs sit together.
- **DTOs as Java records**, mapped with small static `from(...)` methods. Entities never leave the service layer, and `spring.jpa.open-in-view` is off, so lazy loading cannot leak into the web layer.
- **Flyway owns the schema**; Hibernate only validates it (`ddl-auto: validate`).
- **N+1 avoided** on the employee list with an `@EntityGraph` that fetches department and manager in the same query.
- **Search built with JPA Specifications**, so optional filters combine into one query. User input is escaped before it is used in `LIKE`.
- **Integration tests run against real PostgreSQL** (Testcontainers), not an in-memory database, so unique indexes and SQL behave exactly as in production.
- **The leave balance is calculated, not stored.** It is derived from the hire date, birth date and leave requests, so it cannot drift out of sync, and no yearly batch job is needed to add new entitlement.
- **Business rules as pure classes.** `AnnualLeavePolicy` and `WorkingDays` have no Spring or database dependencies and are covered by plain unit tests (including parameterised boundary cases of Art. 53).
- **State transitions live in the entity.** `LeaveRequest.approve / reject / cancel` are the only way to change the status, so an invalid transition cannot be written anywhere else.
- **Concurrency.** Submitting leave locks the employee row (`SELECT … FOR UPDATE`) so two parallel requests cannot spend the same balance; `@Version` optimistic locking stops two managers from deciding the same request at once.
- **An injectable `Clock`** (Europe/Istanbul) instead of `LocalDate.now()`: integration tests pin "today" to a fixed date, so date-dependent rules are tested deterministically.

## Project structure

```
src/main/java/com/batuhanturnay/leaveflow
├── common       base entity, auditing, clock, global error handling
├── calendar     public holidays and working-day calculation
├── department   department API
├── employee     employee API
└── leave        leave requests, approval workflow, entitlement and balance
src/main/resources/db/migration   Flyway migrations
src/test/java/...                 unit tests for business rules, API integration tests (MockMvc + Testcontainers)
compose.yaml                      local PostgreSQL
.github/workflows/ci.yml          build + test on every push / PR
```

## Roadmap

- [x] **Phase 0 – Skeleton:** Maven project, PostgreSQL via Docker Compose, Flyway, Actuator, CI
- [x] **Phase 1 – Core domain:** Employee and Department, DTOs, validation, global error handling (`ProblemDetail`), pagination
- [x] **Phase 2 – Leave workflow:** leave request state machine, entitlement calculation, overlap check, working-day calculation
- [ ] **Phase 3 – Security:** JWT authentication, roles (EMPLOYEE / MANAGER / HR), team-scoped approvals
- [ ] **Phase 4 – Testing:** unit tests for business rules, integration tests with Testcontainers, coverage report
- [ ] **Phase 5 – Extras:** scheduled yearly accrual, reporting endpoints, CSV/Excel export, auditing
- [ ] **Phase 6 – Showcase:** ~~OpenAPI docs~~ (done early: Swagger UI), ER diagram, design decisions, live demo
