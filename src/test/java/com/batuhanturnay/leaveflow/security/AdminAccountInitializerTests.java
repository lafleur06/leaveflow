package com.batuhanturnay.leaveflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.batuhanturnay.leaveflow.IntegrationTest;
import com.batuhanturnay.leaveflow.department.DepartmentRepository;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.employee.Role;

/**
 * The bootstrap is switched off for the shared test context, so each test runs it by hand
 * against the real database with the settings it needs. Every test starts with no HR account.
 */
class AdminAccountInitializerTests extends IntegrationTest {

	private static final String PASSWORD = "first-hr-password";

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock;

	@Test
	void createsTheFirstHrAccountThatCanLogIn() throws Exception {
		runWith(" HR@LeaveFlow.local ", PASSWORD);

		Employee admin = employeeRepository.findByEmailIgnoreCase("hr@leaveflow.local").orElseThrow();
		assertThat(admin.getRole()).isEqualTo(Role.HR);
		assertThat(admin.getEmail()).isEqualTo("hr@leaveflow.local");
		assertThat(admin.getHireDate()).isEqualTo(TODAY);
		assertThat(admin.getDepartment().getName()).isEqualTo("Human Resources");

		noAuthMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "hr@leaveflow.local", "password": "%s" }
				""".formatted(PASSWORD))).andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	void generatesAPasswordWhenNoneIsConfigured() {
		runWith("hr@leaveflow.local", "");

		Employee admin = employeeRepository.findByEmailIgnoreCase("hr@leaveflow.local").orElseThrow();
		assertThat(admin.getPasswordHash()).isNotBlank();
		assertThat(passwordEncoder.matches("", admin.getPasswordHash())).isFalse();
	}

	@Test
	void promotesAnExistingEmployeeAndReusesTheirDepartment() throws Exception {
		long people = createDepartment("Human Resources");
		long ayse = createEmployee("Ayse", "ayse@example.com", people, null);

		runWith("Ayse@example.com", PASSWORD);

		Employee promoted = employeeRepository.findById(ayse).orElseThrow();
		assertThat(promoted.getRole()).isEqualTo(Role.HR);
		assertThat(promoted.getFirstName()).isEqualTo("Ayse");
		assertThat(passwordEncoder.matches(PASSWORD, promoted.getPasswordHash())).isTrue();
		assertThat(departmentRepository.count()).isEqualTo(1);
	}

	@Test
	void doesNothingOnceAnHrAccountExists() throws Exception {
		long people = createDepartment("People");
		createHrEmployee("Selin", "selin@example.com", people);

		runWith("hr@leaveflow.local", PASSWORD);

		assertThat(employeeRepository.findByEmailIgnoreCase("hr@leaveflow.local")).isEmpty();
	}

	@Test
	void isSwitchedOffByAnEmptyEmail() {
		runWith(" ", PASSWORD);

		assertThat(employeeRepository.existsByRole(Role.HR)).isFalse();
	}

	private void runWith(String email, String password) {
		var properties = new SecurityProperties(null, null, new SecurityProperties.Admin(email, password));
		new AdminAccountInitializer(properties, employeeRepository, departmentRepository, passwordEncoder, clock)
			.run(new DefaultApplicationArguments());
	}

}
