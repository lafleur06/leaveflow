package com.batuhanturnay.leaveflow.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;

import com.batuhanturnay.leaveflow.IntegrationTest;
import com.batuhanturnay.leaveflow.department.DepartmentRepository;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.leave.LeaveRequestRepository;
import com.batuhanturnay.leaveflow.leave.LeaveStatus;

/**
 * The public demo. The seed runs at start-up, before the test's transaction, so it is committed
 * data; this class gets its own application context (and database) because of the profile.
 */
@ActiveProfiles("demo")
class DemoProfileTests extends IntegrationTest {

	private static final String PASSWORD = "leaveflow-demo";

	@Autowired
	private DepartmentRepository departments;

	@Autowired
	private EmployeeRepository employees;

	@Autowired
	private LeaveRequestRepository leaveRequests;

	@Autowired
	private DemoDataInitializer initializer;

	@Test
	void seedsACompanyWithLeaveInEveryState() {
		assertThat(departments.count()).isEqualTo(3);
		assertThat(employees.count()).isEqualTo(6);
		assertThat(leaveRequests.findAll()).extracting(r -> r.getStatus())
			.contains(LeaveStatus.PENDING, LeaveStatus.APPROVED, LeaveStatus.REJECTED);
	}

	@Test
	void seedingTwiceChangesNothing() {
		long before = leaveRequests.count();

		initializer.run(new DefaultApplicationArguments());

		assertThat(employees.count()).isEqualTo(6);
		assertThat(leaveRequests.count()).isEqualTo(before);
	}

	@Test
	void loginPageGetsTheDemoAccountsWithoutLoggingIn() throws Exception {
		noAuthMvc.perform(get("/api/demo/accounts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].email").value(DemoDataInitializer.EMPLOYEE_EMAIL))
			.andExpect(jsonPath("$[0].password").value(PASSWORD));
	}

	@Test
	void everyDemoAccountCanLogInAndTheManagerHasAQueue() throws Exception {
		for (String email : DemoDataInitializer.LOGIN_ACCOUNTS) {
			login(email).andExpect(status().isOk());
		}
		String token = token(DemoDataInitializer.MANAGER_EMAIL);
		long managerId = employees.findByEmailIgnoreCase(DemoDataInitializer.MANAGER_EMAIL).orElseThrow().getId();

		noAuthMvc
			.perform(get("/api/leave-requests").param("managerId", String.valueOf(managerId))
				.param("status", "PENDING")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(jsonPath("$.content", hasSize(2)));
	}

	@Test
	void demoAccountsAreProtectedButOtherEmployeesAreNot() throws Exception {
		long ayse = employees.findByEmailIgnoreCase(DemoDataInitializer.EMPLOYEE_EMAIL).orElseThrow().getId();
		long can = employees.findByEmailIgnoreCase("can.demir@leaveflow.demo").orElseThrow().getId();
		String hr = "Bearer " + token(DemoDataInitializer.HR_EMAIL);

		noAuthMvc.perform(put("/api/employees/{id}/password", ayse).header(HttpHeaders.AUTHORIZATION, hr)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "newPassword": "taken-over-1" }
					""")).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value(
					org.hamcrest.Matchers.containsString("demo accounts cannot be changed")));
		noAuthMvc.perform(delete("/api/employees/{id}", ayse).header(HttpHeaders.AUTHORIZATION, hr))
			.andExpect(status().isForbidden());
		noAuthMvc
			.perform(put("/api/me/password").header(HttpHeaders.AUTHORIZATION, hr)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "currentPassword": "%s", "newPassword": "taken-over-1" }
						""".formatted(PASSWORD)))
			.andExpect(status().isForbidden());

		noAuthMvc.perform(put("/api/employees/{id}/password", can).header(HttpHeaders.AUTHORIZATION, hr)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "newPassword": "a-new-password" }
					""")).andExpect(status().isNoContent());
		login(DemoDataInitializer.EMPLOYEE_EMAIL).andExpect(status().isOk());
	}

	private ResultActions login(String email) throws Exception {
		return noAuthMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, PASSWORD)));
	}

	private String token(String email) throws Exception {
		return JsonPath.read(login(email).andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

}
