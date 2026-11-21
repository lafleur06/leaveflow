package com.batuhanturnay.leaveflow;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.batuhanturnay.leaveflow.employee.Role;

/**
 * Base class for API tests: full application context, real PostgreSQL in a container,
 * and every test rolled back afterwards so tests never see each other's data.
 * The Spring context (and so the container) is shared by all subclasses.
 * <p>
 * Requests sent through {@link #mockMvc} act as an HR user by default, so tests about other
 * features do not have to log in first. Add {@code .with(as(employeeId))} to act as a specific
 * employee, or use {@link #noAuthMvc} to test real authentication.
 */
@SpringBootTest(properties = "leaveflow.security.admin.email=") // no bootstrap HR account in tests
@Import({ TestcontainersConfiguration.class, IntegrationTest.FixedClockConfig.class })
@Testcontainers(disabledWithoutDocker = true)
@Transactional
public abstract class IntegrationTest {

	/** "Today" in every integration test: Monday 15 June 2026, 09:00 in Istanbul. */
	public static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

	/** Subject of the default HR identity; deliberately not an existing employee id. */
	private static final long DEFAULT_HR_ID = 999_999L;

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			ZoneId zone = ZoneId.of("Europe/Istanbul");
			return Clock.fixed(TODAY.atTime(9, 0).atZone(zone).toInstant(), zone);
		}

	}

	@Autowired
	private WebApplicationContext context;

	/** Sends requests as an HR user unless the request says otherwise. */
	protected MockMvc mockMvc;

	/** Sends requests with no identity at all: for login and real-token tests. */
	protected MockMvc noAuthMvc;

	@BeforeEach
	void setUpMockMvc() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(springSecurity())
			.defaultRequest(get("/").with(as(DEFAULT_HR_ID, Role.HR)))
			.build();
		noAuthMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	/** Acts as a regular employee. */
	protected static RequestPostProcessor as(long employeeId) {
		return as(employeeId, Role.EMPLOYEE);
	}

	/** Acts as the given employee with the given role, like a token issued by the login endpoint. */
	protected static RequestPostProcessor as(long employeeId, Role role) {
		return jwt().jwt(token -> token.subject(String.valueOf(employeeId)).claim("role", role.name()))
			.authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	protected long createDepartment(String name) throws Exception {
		return postAndReturnId("/api/departments", """
				{ "name": "%s" }
				""".formatted(name));
	}

	protected long createEmployee(String firstName, String email, long departmentId, Long managerId)
			throws Exception {
		return createEmployee(firstName, email, departmentId, managerId, "1990-05-10", "2020-01-06");
	}

	protected long createEmployee(String firstName, String email, long departmentId, Long managerId,
			String birthDate, String hireDate) throws Exception {
		return postAndReturnId("/api/employees",
				employeeJson(firstName, "Test", email, departmentId, managerId, birthDate, hireDate));
	}

	/** Creates an employee and promotes them to HR. */
	protected long createHrEmployee(String firstName, String email, long departmentId) throws Exception {
		long id = createEmployee(firstName, email, departmentId, null);
		mockMvc.perform(put("/api/employees/{id}/role", id).contentType(MediaType.APPLICATION_JSON).content("""
				{ "role": "HR" }
				""")).andExpect(status().isOk());
		return id;
	}

	protected void setPassword(long employeeId, String password) throws Exception {
		mockMvc.perform(put("/api/employees/{id}/password", employeeId).contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "newPassword": "%s" }
					""".formatted(password))).andExpect(status().isNoContent());
	}

	protected static String employeeJson(String firstName, String lastName, String email, long departmentId,
			Long managerId, String birthDate, String hireDate) {
		return """
				{
				  "firstName": "%s",
				  "lastName": "%s",
				  "email": "%s",
				  "jobTitle": "Engineer",
				  "birthDate": "%s",
				  "hireDate": "%s",
				  "departmentId": %d,
				  "managerId": %s
				}
				""".formatted(firstName, lastName, email, birthDate, hireDate, departmentId, managerId);
	}

	protected long postAndReturnId(String url, String json) throws Exception {
		return postAndReturnId(url, json, as(DEFAULT_HR_ID, Role.HR));
	}

	protected long postAndReturnId(String url, String json, RequestPostProcessor identity) throws Exception {
		String body = mockMvc.perform(post(url).with(identity).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}
