package com.batuhanturnay.leaveflow.department;

import java.net.URI;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.batuhanturnay.leaveflow.department.dto.DepartmentRequest;
import com.batuhanturnay.leaveflow.department.dto.DepartmentResponse;

@Tag(name = "Departments", description = "Organisational units that employees belong to")
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

	private final DepartmentService departmentService;

	public DepartmentController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	@Operation(summary = "List departments")
	@GetMapping
	public Page<DepartmentResponse> list(@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
		return departmentService.findAll(pageable);
	}

	@Operation(summary = "Get a department")
	@GetMapping("/{id}")
	public DepartmentResponse get(@PathVariable Long id) {
		return departmentService.findById(id);
	}

	@Operation(summary = "Create a department (HR)")
	@PreAuthorize("hasRole('HR')")
	@PostMapping
	public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
		DepartmentResponse created = departmentService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(created.id())
			.toUri();
		return ResponseEntity.created(location).body(created);
	}

	@Operation(summary = "Replace a department (HR)")
	@PreAuthorize("hasRole('HR')")
	@PutMapping("/{id}")
	public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
		return departmentService.update(id, request);
	}

	@Operation(summary = "Delete a department (only when it has no employees) (HR)")
	@PreAuthorize("hasRole('HR')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		departmentService.delete(id);
	}

}
