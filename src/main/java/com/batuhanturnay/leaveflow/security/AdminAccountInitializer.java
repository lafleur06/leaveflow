package com.batuhanturnay.leaveflow.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.department.Department;
import com.batuhanturnay.leaveflow.department.DepartmentRepository;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.employee.Role;

/**
 * Solves the chicken-and-egg problem of a fresh installation: only HR can create accounts,
 * so the first HR account is created here, once, when none exists.
 * <p>
 * If an employee with the configured email already exists, they are promoted to HR instead.
 * The new account has a placeholder birth date; HR should correct the profile after logging in.
 */
@Component
class AdminAccountInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

	private static final String HR_DEPARTMENT = "Human Resources";

	private final SecurityProperties properties;

	private final EmployeeRepository employeeRepository;

	private final DepartmentRepository departmentRepository;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	AdminAccountInitializer(SecurityProperties properties, EmployeeRepository employeeRepository,
			DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder, Clock clock) {
		this.properties = properties;
		this.employeeRepository = employeeRepository;
		this.departmentRepository = departmentRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String email = properties.admin().email();
		if (email == null || email.isBlank() || employeeRepository.existsByRole(Role.HR)) {
			return;
		}
		email = email.strip().toLowerCase(Locale.ROOT);
		String password = properties.admin().password();
		boolean generated = password == null || password.isBlank();
		if (generated) {
			password = randomPassword();
		}

		Employee admin = employeeRepository.findByEmailIgnoreCase(email).orElse(null);
		if (admin == null) {
			Department hr = departmentRepository.findByNameIgnoreCase(HR_DEPARTMENT)
				.orElseGet(() -> departmentRepository.save(new Department(HR_DEPARTMENT, "People and leave management")));
			admin = new Employee("HR", "Admin", email, "HR Administrator", LocalDate.now(clock),
					LocalDate.of(1990, 1, 1), hr, null);
		}
		admin.changeRole(Role.HR);
		admin.changePasswordHash(passwordEncoder.encode(password));
		employeeRepository.save(admin);

		if (generated) {
			log.warn("Created the first HR account '{}' with the generated password: {}  "
					+ "(log in and change it with PUT /api/me/password, or set LEAVEFLOW_ADMIN_PASSWORD)", email,
					password);
		}
		else {
			log.info("Created the first HR account '{}'", email);
		}
	}

	private static String randomPassword() {
		byte[] bytes = new byte[12];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
