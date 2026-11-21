# LeaveFlow

![CI](https://github.com/lafleur06/leaveflow/actions/workflows/ci.yml/badge.svg)

An HR leave management REST API built with Java 21 and Spring Boot. Employees request leave, managers approve or reject it for their own team, and HR manages staff and entitlements. Annual leave entitlement follows the Turkish Labour Law (Law No. 4857, Art. 53).

> **Status:** Phase 0 – project skeleton. See the [roadmap](#roadmap).

## Tech stack

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 4.2, Maven (wrapper) |
| Persistence | Spring Data JPA (Hibernate), PostgreSQL 17, Flyway migrations |
| API | Spring Web MVC, Bean Validation |
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

Health check: <http://localhost:8080/actuator/health>

### Run the tests

```bash
./mvnw verify
```

Integration tests start a throwaway PostgreSQL container through Testcontainers. On machines without Docker they are skipped, not failed. CI always runs them.

## Project structure

```
src/main/java/com/batuhanturnay/leaveflow   application code
src/main/resources/application.yml           configuration
src/main/resources/db/migration              Flyway migrations (V1__..., V2__...)
src/test/java/...                            tests and Testcontainers setup
compose.yaml                                 local PostgreSQL
.github/workflows/ci.yml                     build + test on every push / PR
```

## Roadmap

- [x] **Phase 0 – Skeleton:** Maven project, PostgreSQL via Docker Compose, Flyway, Actuator, CI
- [ ] **Phase 1 – Core domain:** Employee and Department, DTOs, validation, global error handling (`ProblemDetail`), pagination
- [ ] **Phase 2 – Leave workflow:** leave request state machine, entitlement calculation, overlap check, working-day calculation
- [ ] **Phase 3 – Security:** JWT authentication, roles (EMPLOYEE / MANAGER / HR), team-scoped approvals
- [ ] **Phase 4 – Testing:** unit tests for business rules, integration tests with Testcontainers, coverage report
- [ ] **Phase 5 – Extras:** scheduled yearly accrual, reporting endpoints, CSV/Excel export, auditing
- [ ] **Phase 6 – Showcase:** OpenAPI docs, ER diagram, design decisions, live demo
