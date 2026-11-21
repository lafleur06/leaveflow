# Data model

The tables are created by the Flyway migrations in `src/main/resources/db/migration`.

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

- There is no balance table. The balance is calculated from the hire date, birth date and leave requests.
- The number of working days is saved when a request is created, so adding a holiday later doesn't change old requests.
- `leave_request_events` only gets new rows, nothing is updated or deleted.
- Some rules are also checked in the database (dates, allowed status values, an employee can't be their own manager).
