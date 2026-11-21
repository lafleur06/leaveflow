# LeaveFlow

![CI](https://github.com/lafleur06/leaveflow/actions/workflows/ci.yml/badge.svg)

An HR leave management REST API built with Java 21 and Spring Boot, with a small React client. Employees request leave, managers approve or reject it for their own team, and HR manages staff and entitlements. Annual leave entitlement follows the Turkish Labour Law (Law No. 4857, Art. 53).

> **Status:** Phases 0–5 done, plus a React frontend. 132 tests, 97% line coverage. See the [roadmap](#roadmap).

## Tech stack

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 4.2, Maven (wrapper) |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL 17, Flyway migrations |
| Reports | CSV (hand-written, RFC 4180) and Excel (Apache POI) through Spring MVC content negotiation |
| API | Spring Web MVC, Bean Validation, springdoc-openapi (Swagger UI) |
| Security | Spring Security, OAuth2 Resource Server (JWT, HMAC-SHA256), BCrypt |
| Ops | Spring Boot Actuator, Docker Compose |
| Testing | JUnit 5, Testcontainers, Spring Security Test, JaCoCo (coverage gate) |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query ([details](frontend/README.md)) |
| CI | GitHub Actions (backend build, tests and coverage; frontend lint and build) |

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

### First login

On the first start, when no HR account exists, the application creates one:

- email: `hr@leaveflow.local` (override with `LEAVEFLOW_ADMIN_EMAIL`)
- password: the value of `LEAVEFLOW_ADMIN_PASSWORD`, or, when it is not set, a random password **printed once in the startup log** (`Created the first HR account ...`)

Then, in Swagger UI:

1. `POST /api/auth/login` with that email and password, and copy the `accessToken`.
2. Click **Authorize** and paste the token.
3. As HR, create employees and give them a password with `PUT /api/employees/{id}/password`, so they can log in too.

### Run the frontend

With the API running, in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> and log in. Employees see their balance as a strip of days, request and cancel leave; managers approve or reject their team's requests under **Team approvals**. HR administration (employees, roles, passwords, holidays) is done through Swagger UI for now.

### Configuration

| Environment variable | Purpose | Default |
|---|---|---|
| `LEAVEFLOW_JWT_SECRET` | HMAC key for signing tokens, at least 32 characters | random per start (tokens stop working after a restart) |
| `LEAVEFLOW_ADMIN_EMAIL` | Email of the bootstrap HR account | `hr@leaveflow.local` |
| `LEAVEFLOW_ADMIN_PASSWORD` | Its initial password | random, printed in the log |

Secrets are never stored in the repository.

### Run the tests

```bash
./mvnw verify
```

Integration tests start a throwaway PostgreSQL container through Testcontainers. On machines without Docker they are skipped, not failed. CI always runs them.

`verify` also measures coverage with JaCoCo and writes the report to `target/site/jacoco/index.html`. The build fails if line coverage drops below 90% or branch coverage below 85% (currently about 97% and 95%). Without Docker the integration tests are skipped, so coverage falls under the limit; add `-Djacoco.skip=true` in that case. In CI the coverage table appears in the job summary and the HTML report is attached to the run as `coverage-report`.

What the tests cover:

- **Business rules as unit tests:** Art. 53 entitlement boundaries (service years, age 18 and 50), working days around weekends and public holidays.
- **Every endpoint through MockMvc against real PostgreSQL:** validation and error bodies, duplicates, 404s, role and team checks, the leave state machine (approve, reject, cancel at each status), balance edge cases such as booking exactly the remaining days, the audit trail (refused changes leave no trace), and every report in all three formats, with the files parsed back in the test.
- **Authentication end to end:** real login tokens, wrong passwords, expired or forged tokens, the bootstrap HR account and JWT key setup.

## API

Interactive documentation is available at `/swagger-ui.html` while the application is running. Every endpoint except login, the API docs and the health check needs an `Authorization: Bearer <token>` header.

| Method | Path | Who | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | anyone | Log in with email and password; returns an access token (1 hour) |
| `GET` | `/api/me` | logged in | Your own profile |
| `PUT` | `/api/me/password` | logged in | Change your own password |
| `GET` | `/api/departments`, `/api/departments/{id}` | logged in | List (paged) or get departments |
| `POST` `PUT` `DELETE` | `/api/departments/...` | HR | Manage departments (delete only when empty) |
| `GET` | `/api/employees` | logged in | Search: `departmentId`, `active`, `q` (name or email), paging |
| `GET` | `/api/employees/{id}` | logged in | Get one employee (birth date only for HR and the employee) |
| `POST` `PUT` `DELETE` | `/api/employees/...` | HR | Create, replace, deactivate (soft delete) |
| `PUT` | `/api/employees/{id}/role` | HR | Change the role (`EMPLOYEE` or `HR`), not your own |
| `PUT` | `/api/employees/{id}/password` | HR | Set an initial password or reset a forgotten one |
| `GET` | `/api/employees/{id}/leave-balance` | employee, manager, HR | Earned, used, pending and available days, next accrual |
| `GET` | `/api/leave-requests` | logged in | Search: `employeeId`, `managerId`, `status`, `type`, paging. You see your own and your team's; HR sees all |
| `GET` | `/api/leave-requests/{id}` | employee, manager, HR | Get one leave request |
| `GET` | `/api/leave-requests/{id}/history` | employee, manager, HR | Audit trail: who submitted, approved, rejected or cancelled it, when, with which note |
| `POST` | `/api/leave-requests` | logged in | Submit leave for yourself (`ANNUAL`, `SICK` or `UNPAID`) |
| `POST` | `/api/leave-requests/{id}/approve` | direct manager | Approve (HR for employees without a manager) |
| `POST` | `/api/leave-requests/{id}/reject` | direct manager | Reject with a note (HR for employees without a manager) |
| `POST` | `/api/leave-requests/{id}/cancel` | employee, HR | Cancel a pending request, or an approved one before it starts |
| `GET` | `/api/holidays?year=2026` | logged in | Public holidays of a year |
| `POST` `DELETE` | `/api/holidays/...` | HR | Add or remove a public holiday |
| `GET` | `/api/reports/leave-balances` | HR | Annual leave balance of every active employee today; `departmentId` |
| `GET` | `/api/reports/leave-usage?year=2026` | HR | Approved leave per department and type (requests, employees, working days) |
| `GET` | `/api/reports/absences?from=&to=` | HR | Who is away: approved leave overlapping the period (default: the next 30 days) |

Reports are JSON by default. Ask for `Accept: text/csv` or the Excel type (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`), or simply add `?format=csv` / `?format=xlsx`, to download a file:

```bash
curl -H "Authorization: Bearer $TOKEN" -OJ "localhost:8080/api/reports/leave-balances?format=xlsx"
# saves leave-balances-<today>.xlsx
```

Example:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"ayse.yilmaz@example.com","password":"..."}' | jq -r .accessToken)

curl -X POST localhost:8080/api/leave-requests -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":"ANNUAL","startDate":"2026-12-14","endDate":"2026-12-18","reason":"Family visit"}'
```

Paged responses look like `{ "content": [ ... ], "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 } }`.

### Roles and permissions

- **EMPLOYEE** – sees the colleague directory (without birth dates), their own profile, their own leave and balance; submits and cancels their own leave.
- **Manager** is *not* a stored role: anyone with direct reports decides on those reports' leave and sees their requests and balances. Permissions therefore always follow the real reporting lines.
- **HR** – manages employees, departments, holidays, roles and passwords; sees all leave; decides on leave of employees who have no manager. HR does not overrule a manager, and nobody decides on their own leave.

### Business rules

- Department names and employee emails are unique, case-insensitively (enforced in the service **and** by unique indexes).
- An employee must be at least 15 on the hire date (Labour Law No. 4857, Art. 71).
- An employee cannot manage themselves, an inactive employee cannot be a manager, and reporting cycles (A → B → A) are rejected.
- Employees are never hard-deleted, because their leave history must survive. Deactivation is blocked while they still manage active staff, and deactivated employees can no longer log in.
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
- A rejection needs a note.

**Public holidays.** The fixed-date national holidays of 2026–2027 and the religious holidays of 2026 are seeded by Flyway. Religious holidays move every year, so HR adds the future ones through `POST /api/holidays` once they are announced. Half-day eves (arife, 28 October afternoon) are not modelled yet.

### Errors

Every error is an [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json` response, including security errors raised before a controller runs:

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
| 401 | Missing, invalid or expired token; wrong email or password (one message for every login failure) |
| 403 | Role not allowed (e.g. an employee creating a department), or not your data (someone else's leave, deciding without being the direct manager) |
| 404 | Resource not found |
| 409 | Duplicate name/email/holiday, deleting a department that has employees, deactivating a manager with active reports, overlapping leave, invalid status transition, concurrent update |
| 422 | Valid request that breaks a business rule (minimum age, reporting cycle, insufficient balance, no working days in the period, leave in the past, wrong current password, changing your own role) |

Validation messages are in English by default; send `Accept-Language: tr` to get them in Turkish.

## Design decisions

- **Feature-based packages** (`department`, `employee`, `leave`, ...) instead of layer-based ones, so each feature's controller, service, repository and DTOs sit together.
- **DTOs as Java records**, mapped with small static `from(...)` methods. Entities never leave the service layer, and `spring.jpa.open-in-view` is off, so lazy loading cannot leak into the web layer.
- **Flyway owns the schema**; Hibernate only validates it (`ddl-auto: validate`).
- **N+1 avoided** on list endpoints with `@EntityGraph`s that fetch related records in the same query.
- **Search built with JPA Specifications**, so optional filters combine into one query. User input is escaped before it is used in `LIKE`.
- **Integration tests run against real PostgreSQL** (Testcontainers), not an in-memory database, so unique indexes and SQL behave exactly as in production.
- **The leave balance is calculated, not stored.** It is derived from the hire date, birth date and leave requests, so it cannot drift out of sync, and no yearly batch job is needed to add new entitlement.
- **Business rules as pure classes.** `AnnualLeavePolicy` and `WorkingDays` have no Spring or database dependencies and are covered by plain unit tests (including parameterised boundary cases of Art. 53).
- **State transitions live in the entity.** `LeaveRequest.approve / reject / cancel` are the only way to change the status, so an invalid transition cannot be written anywhere else.
- **Concurrency.** Submitting leave locks the employee row (`SELECT … FOR UPDATE`) so two parallel requests cannot spend the same balance; `@Version` optimistic locking stops two managers from deciding the same request at once.
- **An audit trail written by domain events.** `LeaveRequest` raises a `LeaveStatusChanged` event on every change, including the submission; Spring Data publishes it on `save` (`@DomainEvents`), and a synchronous listener appends a row to `leave_request_events` in the same transaction. The history therefore cannot miss a change, and a change cannot be committed without its history row. The table is append-only (`@Immutable`). Its migration rebuilt the history of requests that already existed from their stored decision fields.
- **One balance rule for every caller.** The HR balance report loads all approved and pending days with one `GROUP BY` query and then applies the same `LeaveBalanceService.balanceOf` as the single-employee endpoint, so the two can never disagree and the report needs two queries however many employees there are.
- **Exports through content negotiation.** Report DTOs implement `Exportable`; two `HttpMessageConverter`s turn them into CSV or Excel, so each report is one controller method and the format is the client's choice. CSV is UTF-8 with a byte order mark (Turkish letters open correctly in Excel), and text that starts with `= + - @` is prefixed with `'` so a crafted name cannot run as a formula (CSV injection). Excel cells are typed: numbers and dates are real numbers and dates, text is never a formula.
- **An injectable `Clock`** (Europe/Istanbul) instead of `LocalDate.now()`: integration tests pin "today" to a fixed date, so date-dependent rules are tested deterministically.

**Security**

- **Stateless JWT** with Spring Security's own OAuth2 Resource Server and Nimbus support, no extra JWT library. The application issues tokens (login) and verifies them (every other endpoint) with one HMAC-SHA256 key. No session and no cookies, so CSRF protection is not needed.
- **Authorization in three layers:** the filter chain decides who must be logged in, `@PreAuthorize("hasRole('HR')")` guards HR-only endpoints, and services check data-dependent rules ("only the direct manager", "only your own leave").
- **The identity comes from the token, never from the request body.** Leave is always submitted for, and decided by, the logged-in user.
- **Login does not leak which emails exist:** every failure returns the same message, and an unknown email is still checked against a dummy BCrypt hash so the response time is the same.
- **Passwords** are hashed with BCrypt through a delegating encoder (`{bcrypt}` prefix), so the algorithm can be upgraded later; the 72-byte BCrypt limit is enforced by validation.
- **Personal data (KVKK).** Birth dates are returned only to HR and to the employee themselves.
- **A bootstrap HR account** solves the first-login problem without committing a password: it comes from an environment variable or is generated and logged once.
- **Known limitation:** a token stays valid until it expires (1 hour) even if the employee is deactivated or their role changes in the meantime; refresh tokens and revocation are not implemented.

## Project structure

```
src/main/java/com/batuhanturnay/leaveflow
├── common       base entity, auditing, clock, OpenAPI, global error handling, CSV/Excel export
├── security     login, JWT issuing and verification, security rules, bootstrap HR account
├── calendar     public holidays and working-day calculation
├── department   department API
├── employee     employee API, roles and passwords
├── leave        leave requests, approval workflow, entitlement, balance and audit trail
└── report       HR reports (balances, usage, absences)
src/main/resources/db/migration   Flyway migrations
src/test/java/...                 unit tests for business rules, API integration tests (MockMvc + Testcontainers)
frontend/                         React client (Vite dev server proxies /api to :8080)
compose.yaml                      local PostgreSQL
.github/workflows/ci.yml          build, test and coverage on every push / PR
```

## Roadmap

- [x] **Phase 0 – Skeleton:** Maven project, PostgreSQL via Docker Compose, Flyway, Actuator, CI
- [x] **Phase 1 – Core domain:** Employee and Department, DTOs, validation, global error handling (`ProblemDetail`), pagination
- [x] **Phase 2 – Leave workflow:** leave request state machine, entitlement calculation, overlap check, working-day calculation
- [x] **Phase 3 – Security:** JWT authentication, roles (EMPLOYEE / HR, managers derived from reporting lines), team-scoped approvals
- [x] **Frontend:** a small React client for login, my leave, new request and the manager's approval queue (HR screens still to come)
- [x] **Phase 4 – Testing:** JaCoCo coverage report with a build gate, edge-case tests for the leave state machine, employee updates and the security bootstrap
- [x] **Phase 5 – Reports and audit:** HR reports (balances, usage, absences) as JSON, CSV or Excel; audit trail of every leave status change
- [ ] **Phase 6 – Showcase:** ~~OpenAPI docs~~ (done early: Swagger UI), ER diagram, live demo
