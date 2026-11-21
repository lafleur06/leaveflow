package com.batuhanturnay.leaveflow.employee;

import java.net.URI;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.batuhanturnay.leaveflow.employee.dto.EmployeeRequest;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeResponse;
import com.batuhanturnay.leaveflow.employee.dto.RoleChangeRequest;
import com.batuhanturnay.leaveflow.employee.dto.SetPasswordRequest;

@Tag(name = "Employees", description = "Staff records, reporting lines and deactivation")
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

	private final EmployeeService employeeService;

	public EmployeeController(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	/**
	 * Example: {@code GET /api/employees?departmentId=1&active=true&q=ayse&sort=hireDate,desc}
	 */
	@Operation(summary = "Search employees by department, active flag and name or email")
	@GetMapping
	public Page<EmployeeResponse> list(@RequestParam(required = false) Long departmentId,
			@RequestParam(required = false) Boolean active, @RequestParam(name = "q", required = false) String query,
			@ParameterObject @PageableDefault(size = 20, sort = { "lastName", "firstName" }) Pageable pageable) {
		return employeeService.search(departmentId, active, query, pageable);
	}

	@Operation(summary = "Get an employee")
	@GetMapping("/{id}")
	public EmployeeResponse get(@PathVariable Long id) {
		return employeeService.findById(id);
	}

	@Operation(summary = "Create an employee (HR)")
	@PreAuthorize("hasRole('HR')")
	@PostMapping
	public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
		EmployeeResponse created = employeeService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@Operation(summary = "Replace an employee (HR)")
	@PreAuthorize("hasRole('HR')")
	@PutMapping("/{id}")
	public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
		return employeeService.update(id, request);
	}

	/** Soft delete: the employee is deactivated, their history is kept. */
	@Operation(summary = "Deactivate an employee (soft delete, HR)")
	@PreAuthorize("hasRole('HR')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivate(@PathVariable Long id) {
		employeeService.deactivate(id);
	}

	@Operation(summary = "Change an employee's role (HR; not your own)")
	@PreAuthorize("hasRole('HR')")
	@PutMapping("/{id}/role")
	public EmployeeResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest request) {
		return employeeService.changeRole(id, request.role());
	}

	@Operation(summary = "Set or reset an employee's password (HR)")
	@PreAuthorize("hasRole('HR')")
	@PutMapping("/{id}/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void setPassword(@PathVariable Long id, @Valid @RequestBody SetPasswordRequest request) {
		employeeService.setPassword(id, request.newPassword());
	}

}
