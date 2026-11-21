package com.batuhanturnay.leaveflow.department;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.batuhanturnay.leaveflow.common.persistence.BaseEntity;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 255)
	private String description;

	protected Department() {
		// for JPA
	}

	public Department(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

}
