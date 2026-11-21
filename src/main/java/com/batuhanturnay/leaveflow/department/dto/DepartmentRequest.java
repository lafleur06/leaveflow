package com.batuhanturnay.leaveflow.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating or replacing a department. */
public record DepartmentRequest(

		@NotBlank @Size(max = 100) String name,

		@Size(max = 255) String description) {

}
