package com.batuhanturnay.leaveflow.employee.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.batuhanturnay.leaveflow.employee.Employee;

public record EmployeeResponse(Long id, String firstName, String lastName, String email, String jobTitle,
		LocalDate hireDate, LocalDate birthDate, boolean active, Ref department, Ref manager, Instant createdAt,
		Instant updatedAt) {

	/** A lightweight pointer to a related record: enough to display it and to link to it. */
	public record Ref(Long id, String name) {

	}

	public static EmployeeResponse from(Employee e) {
		Employee manager = e.getManager();
		return new EmployeeResponse(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getJobTitle(),
				e.getHireDate(), e.getBirthDate(), e.isActive(),
				new Ref(e.getDepartment().getId(), e.getDepartment().getName()),
				manager == null ? null : new Ref(manager.getId(), manager.getFullName()), e.getCreatedAt(),
				e.getUpdatedAt());
	}

}
