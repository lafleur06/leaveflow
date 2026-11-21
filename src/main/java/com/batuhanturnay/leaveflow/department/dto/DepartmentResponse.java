package com.batuhanturnay.leaveflow.department.dto;

import java.time.Instant;

import com.batuhanturnay.leaveflow.department.Department;

public record DepartmentResponse(Long id, String name, String description, Instant createdAt, Instant updatedAt) {

	public static DepartmentResponse from(Department department) {
		return new DepartmentResponse(department.getId(), department.getName(), department.getDescription(),
				department.getCreatedAt(), department.getUpdatedAt());
	}

}
