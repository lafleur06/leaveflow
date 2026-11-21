package com.batuhanturnay.leaveflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for API tests: full application context, real PostgreSQL in a container,
 * and every test rolled back afterwards so tests never see each other's data.
 * The Spring context (and so the container) is shared by all subclasses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@Transactional
public abstract class IntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

	protected long createDepartment(String name) throws Exception {
		return postAndReturnId("/api/departments", """
				{ "name": "%s" }
				""".formatted(name));
	}

	protected long createEmployee(String firstName, String email, long departmentId, Long managerId)
			throws Exception {
		return postAndReturnId("/api/employees",
				employeeJson(firstName, "Test", email, departmentId, managerId, "1990-05-10", "2020-01-06"));
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

	private long postAndReturnId(String url, String json) throws Exception {
		String body = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) JsonPath.read(body, "$.id")).longValue();
	}

}
