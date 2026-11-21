package com.batuhanturnay.leaveflow.employee;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.common.error.BusinessRuleException;
import com.batuhanturnay.leaveflow.common.error.ConflictException;
import com.batuhanturnay.leaveflow.common.error.ResourceNotFoundException;
import com.batuhanturnay.leaveflow.department.Department;
import com.batuhanturnay.leaveflow.department.DepartmentService;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeRequest;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeResponse;
import com.batuhanturnay.leaveflow.security.AuthenticatedUser;
import com.batuhanturnay.leaveflow.security.CurrentUser;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

	/**
	 * Turkish Labour Law No. 4857, Art. 71: children under 15 may not be employed
	 * (14 only for light work under special conditions, not modelled here).
	 */
	static final int MINIMUM_WORKING_AGE = 15;

	private final EmployeeRepository employeeRepository;

	private final DepartmentService departmentService;

	private final CurrentUser currentUser;

	private final PasswordEncoder passwordEncoder;

	public EmployeeService(EmployeeRepository employeeRepository, DepartmentService departmentService,
			CurrentUser currentUser, PasswordEncoder passwordEncoder) {
		this.employeeRepository = employeeRepository;
		this.departmentService = departmentService;
		this.currentUser = currentUser;
		this.passwordEncoder = passwordEncoder;
	}

	/** Every employee may browse the directory; birth dates are visible to HR only. */
	public Page<EmployeeResponse> search(Long departmentId, Boolean active, String query, Pageable pageable) {
		AuthenticatedUser viewer = currentUser.get();
		return employeeRepository.findAll(EmployeeSpecifications.filter(departmentId, active, query), pageable)
			.map(employee -> visibleTo(viewer, employee));
	}

	public EmployeeResponse findById(Long id) {
		return visibleTo(currentUser.get(), getEntity(id));
	}

	@Transactional
	public EmployeeResponse changeRole(Long id, Role role) {
		Employee employee = getEntity(id);
		if (currentUser.get().is(id)) {
			// stops the last HR account from locking everyone out by demoting itself
			throw new BusinessRuleException("You cannot change your own role");
		}
		employee.changeRole(role);
		return EmployeeResponse.from(employeeRepository.saveAndFlush(employee));
	}

	/** HR sets an initial password or resets a forgotten one. */
	@Transactional
	public void setPassword(Long id, String rawPassword) {
		getEntity(id).changePasswordHash(passwordEncoder.encode(rawPassword));
	}

	/** Personal data (the birth date, under KVKK) is shown only to HR and to the employee themselves. */
	private static EmployeeResponse visibleTo(AuthenticatedUser viewer, Employee employee) {
		EmployeeResponse response = EmployeeResponse.from(employee);
		return viewer.isHr() || viewer.is(employee.getId()) ? response : response.withoutPersonalData();
	}

	@Transactional
	public EmployeeResponse create(EmployeeRequest request) {
		String email = normalizeEmail(request.email());
		if (employeeRepository.existsByEmailIgnoreCase(email)) {
			throw duplicateEmail(email);
		}
		validateWorkingAge(request.birthDate(), request.hireDate());
		Department department = departmentService.getEntity(request.departmentId());
		Employee manager = resolveManager(request.managerId(), null);

		Employee employee = new Employee(request.firstName().strip(), request.lastName().strip(), email,
				request.jobTitle(), request.hireDate(), request.birthDate(), department, manager);
		return EmployeeResponse.from(employeeRepository.save(employee));
	}

	@Transactional
	public EmployeeResponse update(Long id, EmployeeRequest request) {
		Employee employee = getEntity(id);
		String email = normalizeEmail(request.email());
		if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
			throw duplicateEmail(email);
		}
		validateWorkingAge(request.birthDate(), request.hireDate());
		Department department = departmentService.getEntity(request.departmentId());
		Employee manager = resolveManager(request.managerId(), employee);

		employee.update(request.firstName().strip(), request.lastName().strip(), email, request.jobTitle(),
				request.hireDate(), request.birthDate(), department, manager);
		// flush so the response carries the new updatedAt set by auditing
		return EmployeeResponse.from(employeeRepository.saveAndFlush(employee));
	}

	/** Marks the employee as having left. Calling it twice is harmless. */
	@Transactional
	public void deactivate(Long id) {
		Employee employee = getEntity(id);
		if (!employee.isActive()) {
			return;
		}
		if (employeeRepository.existsByManagerIdAndActiveTrue(id)) {
			throw new ConflictException(
					"Employee %d still manages active employees; assign them a new manager first".formatted(id));
		}
		employee.deactivate();
	}

	public Employee getEntity(Long id) {
		return employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee", id));
	}

	/**
	 * @param employee the employee being updated, or {@code null} when creating a new one
	 */
	private Employee resolveManager(Long managerId, Employee employee) {
		if (managerId == null) {
			return null;
		}
		if (employee != null && managerId.equals(employee.getId())) {
			throw new BusinessRuleException("An employee cannot be their own manager");
		}
		Employee manager = employeeRepository.findById(managerId)
			.orElseThrow(() -> new ResourceNotFoundException("Employee", managerId));
		if (!manager.isActive()) {
			throw new BusinessRuleException(
					"Employee %d is no longer active and cannot be a manager".formatted(managerId));
		}
		if (employee != null) {
			// Walk up the new manager's chain: meeting the employee there means A -> B -> A.
			for (Employee current = manager; current != null; current = current.getManager()) {
				if (current.getId().equals(employee.getId())) {
					throw new BusinessRuleException(
							"Employee %d reports (directly or indirectly) to employee %d; this would create a reporting cycle"
								.formatted(managerId, employee.getId()));
				}
			}
		}
		return manager;
	}

	private static void validateWorkingAge(LocalDate birthDate, LocalDate hireDate) {
		if (Period.between(birthDate, hireDate).getYears() < MINIMUM_WORKING_AGE) {
			throw new BusinessRuleException(
					"Employee must be at least %d years old on the hire date".formatted(MINIMUM_WORKING_AGE));
		}
	}

	private static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	private static ConflictException duplicateEmail(String email) {
		return new ConflictException("An employee with email '%s' already exists".formatted(email));
	}

}
