-- Phase 3: authentication. Existing employees become regular employees without a password;
-- HR sets their initial password through PUT /api/employees/{id}/password.

alter table employees
    add column role          varchar(20)  not null default 'EMPLOYEE',
    add column password_hash varchar(100),
    add constraint ck_employees_role check (role in ('EMPLOYEE', 'HR'));
