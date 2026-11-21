package com.batuhanturnay.leaveflow.employee.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.batuhanturnay.leaveflow.common.web.Ref;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.Role;

/**
 * @param birthDate personal data: only returned to HR and to the employee themselves (see
 * {@link #withoutPersonalData()}); {@code null} for everyone else
 * @param hasPassword whether the employee can log in yet
 */
public record EmployeeResponse(Long id, String firstName, String lastName, String email, String jobTitle,
		LocalDate hireDate, LocalDate birthDate, boolean active, Role role, boolean hasPassword, Ref department,
		Ref manager, Instant createdAt, Instant updatedAt) {

	public static EmployeeResponse from(Employee e) {
		return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getJobTitle(),
				e.getHireDate(), e.getBirthDate(), e.isActive(), e.getRole(), e.getPasswordHash() != null,
				new Ref(e.getDepartment().getId(), e.getDepartment().getName()), refTo(e.getManager()),
				e.getCreatedAt(), e.getUpdatedAt());
	}

	/** The colleague-directory view: same record without the birth date. */
	public EmployeeResponse withoutPersonalData() {
		return new EmployeeResponse(id, firstName, lastName, email, jobTitle, hireDate, null, active, role,
				hasPassword, department, manager, createdAt, updatedAt);
	}

	/** Reference to an employee, or {@code null} when there is none. */
	public static Ref refTo(Employee employee) {
		return employee == null ? null : new Ref(employee.getId(), employee.getFullName());
	}

}
