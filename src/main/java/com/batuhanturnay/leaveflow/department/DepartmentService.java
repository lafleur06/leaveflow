package com.batuhanturnay.leaveflow.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.common.error.ConflictException;
import com.batuhanturnay.leaveflow.common.error.ResourceNotFoundException;
import com.batuhanturnay.leaveflow.department.dto.DepartmentRequest;
import com.batuhanturnay.leaveflow.department.dto.DepartmentResponse;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

	private final DepartmentRepository departmentRepository;

	private final EmployeeRepository employeeRepository;

	public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
		this.departmentRepository = departmentRepository;
		this.employeeRepository = employeeRepository;
	}

	public Page<DepartmentResponse> findAll(Pageable pageable) {
		return departmentRepository.findAll(pageable).map(DepartmentResponse::from);
	}

	public DepartmentResponse findById(Long id) {
		return DepartmentResponse.from(getEntity(id));
	}

	@Transactional
	public DepartmentResponse create(DepartmentRequest request) {
		String name = request.name().strip();
		if (departmentRepository.existsByNameIgnoreCase(name)) {
			throw duplicateName(name);
		}
		Department saved = departmentRepository.save(new Department(name, request.description()));
		return DepartmentResponse.from(saved);
	}

	@Transactional
	public DepartmentResponse update(Long id, DepartmentRequest request) {
		Department department = getEntity(id);
		String name = request.name().strip();
		if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw duplicateName(name);
		}
		department.update(name, request.description());
		// flush so the response carries the new updatedAt set by auditing
		return DepartmentResponse.from(departmentRepository.saveAndFlush(department));
	}

	@Transactional
	public void delete(Long id) {
		Department department = getEntity(id);
		if (employeeRepository.existsByDepartmentId(id)) {
			throw new ConflictException(
					"Department %d still has employees; move them to another department first".formatted(id));
		}
		departmentRepository.delete(department);
	}

	/** For other services that need the entity itself, not the DTO. */
	public Department getEntity(Long id) {
		return departmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
	}

	private static ConflictException duplicateName(String name) {
		return new ConflictException("A department named '%s' already exists".formatted(name));
	}

}
