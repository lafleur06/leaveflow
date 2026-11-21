package com.batuhanturnay.leaveflow.employee.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/** Body for creating or replacing an employee. */
public record EmployeeRequest(

		@NotBlank @Size(max = 50) String firstName,

		@NotBlank @Size(max = 50) String lastName,

		@NotBlank @Email @Size(max = 150) String email,

		@Size(max = 100) String jobTitle,

		@NotNull LocalDate hireDate,

		@NotNull @Past LocalDate birthDate,

		@NotNull Long departmentId,

		Long managerId) {

}
