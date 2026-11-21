package com.batuhanturnay.leaveflow.employee;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.batuhanturnay.leaveflow.common.persistence.BaseEntity;
import com.batuhanturnay.leaveflow.department.Department;

@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

	@Column(name = "first_name", nullable = false, length = 50)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 50)
	private String lastName;

	@Column(nullable = false, length = 150)
	private String email;

	@Column(name = "job_title", length = 100)
	private String jobTitle;

	@Column(name = "hire_date", nullable = false)
	private LocalDate hireDate;

	@Column(name = "birth_date", nullable = false)
	private LocalDate birthDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "department_id", nullable = false)
	private Department department;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_id")
	private Employee manager;

	/**
	 * Employees are never hard-deleted: their leave history must survive.
	 * Leaving the company sets this flag to false.
	 */
	@Column(nullable = false)
	private boolean active = true;

	protected Employee() {
		// for JPA
	}

	public Employee(String firstName, String lastName, String email, String jobTitle, LocalDate hireDate,
			LocalDate birthDate, Department department, Employee manager) {
		update(firstName, lastName, email, jobTitle, hireDate, birthDate, department, manager);
	}

	public void update(String firstName, String lastName, String email, String jobTitle, LocalDate hireDate,
			LocalDate birthDate, Department department, Employee manager) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.jobTitle = jobTitle;
		this.hireDate = hireDate;
		this.birthDate = birthDate;
		this.department = department;
		this.manager = manager;
	}

	public void deactivate() {
		this.active = false;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public Department getDepartment() {
		return department;
	}

	public Employee getManager() {
		return manager;
	}

	public boolean isActive() {
		return active;
	}

}
