# Data model

The schema is created by the Flyway migrations in `src/main/resources/db/migration` (V1 to V5).
Hibernate only validates it. GitHub renders the diagram below.

```mermaid
erDiagram
    departments ||--o{ employees : "has"
    employees |o--o{ employees : "manages"
    employees ||--o{ leave_requests : "requests"
    employees |o--o{ leave_requests : "decides"
    leave_requests ||--|{ leave_request_events : "history"
    employees |o--o{ leave_request_events : "acts in"

    departments {
        bigint id PK
        varchar name UK "unique, case-insensitive"
        varchar description
        timestamptz created_at
        timestamptz updated_at
    }

    employees {
        bigint id PK
        varchar first_name
        varchar last_name
        varchar email UK "unique, case-insensitive; login"
        varchar job_title
        date hire_date "drives annual leave entitlement"
        date birth_date "age bonus of Art. 53; HR and self only"
        bigint department_id FK
        bigint manager_id FK "null for top of the hierarchy"
        boolean active "soft delete"
        varchar role "EMPLOYEE or HR"
        varchar password_hash "BCrypt; null until HR sets one"
        timestamptz created_at
        timestamptz updated_at
    }

    leave_requests {
        bigint id PK
        bigint employee_id FK
        varchar type "ANNUAL, SICK, UNPAID"
        date start_date
        date end_date
        int working_days "fixed at submission"
        varchar reason
        varchar status "PENDING, APPROVED, REJECTED, CANCELLED"
        bigint decided_by_id FK
        timestamptz decided_at
        varchar decision_note
        bigint version "optimistic locking"
        timestamptz created_at
        timestamptz updated_at
    }

    leave_request_events {
        bigint id PK
        bigint leave_request_id FK
        varchar action "SUBMITTED, APPROVED, REJECTED, CANCELLED"
        bigint actor_id FK "null only for backfilled cancellations"
        varchar note
        timestamptz occurred_at
    }

    public_holidays {
        bigint id PK
        date holiday_date UK
        varchar name
        timestamptz created_at
        timestamptz updated_at
    }
```

## Notes

- **No balance table.** The annual leave balance is computed from `hire_date`, `birth_date` and the approved and pending `ANNUAL` requests, so it cannot get out of sync.
- **`public_holidays` stands alone.** It is read when a request is submitted: weekends and these dates are not working days. The result is stored in `leave_requests.working_days`, so adding a holiday later does not rewrite past requests.
- **`leave_request_events` is append-only.** The application inserts a row on every status change and never updates or deletes one.
- **Checks in the database as well as in code:**
  - `end_date >= start_date`
  - `working_days > 0`
  - `hire_date > birth_date`
  - an employee is not their own manager
  - the allowed values of `type`, `status`, `role` and `action`

  These still hold when someone edits data with SQL.
- **Indexes:**
  - `(employee_id, start_date, end_date)` on `leave_requests` serves the overlap check.
  - `(leave_request_id, occurred_at)` on `leave_request_events` serves the history endpoint.
  - The foreign keys used for filtering (`department_id`, `manager_id`) are indexed.
