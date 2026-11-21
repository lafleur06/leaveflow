package com.batuhanturnay.leaveflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;

import com.batuhanturnay.leaveflow.IntegrationTest;
import com.batuhanturnay.leaveflow.employee.Role;

/**
 * Real authentication: these tests log in through the API and send the returned token,
 * so token signing, verification and the role claim are all exercised end to end.
 */
class SecurityApiTests extends IntegrationTest {

	private static final String PASSWORD = "correct-horse-42";

	@Autowired
	private JwtEncoder jwtEncoder;

	private long engineering;

	private long ayse;

	@BeforeEach
	void setUp() throws Exception {
		engineering = createDepartment("Engineering");
		ayse = createEmployee("Ayse", "ayse@example.com", engineering, null);
		setPassword(ayse, PASSWORD);
	}

	@Test
	void rejectsRequestsWithoutAToken() throws Exception {
		noAuthMvc.perform(get("/api/employees"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void rejectsAForgedToken() throws Exception {
		noAuthMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.real-token"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsAnExpiredToken() throws Exception {
		Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(String.valueOf(ayse))
			.claim("role", Role.EMPLOYEE.name())
			.issuedAt(twoHoursAgo)
			.expiresAt(twoHoursAgo.plus(Duration.ofHours(1)))
			.build();
		String token = jwtEncoder
			.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();

		noAuthMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logsInAndUsesTheToken() throws Exception {
		login("AYSE@example.com", PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(3600));

		noAuthMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, bearer(token("ayse@example.com", PASSWORD))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(ayse))
			.andExpect(jsonPath("$.role").value("EMPLOYEE"))
			.andExpect(jsonPath("$.birthDate").value("1990-05-10"));
	}

	@Test
	void givesTheSameAnswerForEveryLoginFailure() throws Exception {
		long noPassword = createEmployee("Can", "can@example.com", engineering, null);
		long former = createEmployee("Former", "former@example.com", engineering, null);
		setPassword(former, PASSWORD);
		mockMvc.perform(delete("/api/employees/{id}", former)).andExpect(status().isNoContent());

		for (String[] attempt : new String[][] { { "ayse@example.com", "wrong-password" },
				{ "nobody@example.com", PASSWORD }, { "can@example.com", PASSWORD },
				{ "former@example.com", PASSWORD } }) {
			login(attempt[0], attempt[1]).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Invalid email or password"));
		}
		assertThat(noPassword).isPositive();
	}

	@Test
	void regularEmployeesCannotUseHrEndpoints() throws Exception {
		String token = token("ayse@example.com", PASSWORD);

		noAuthMvc.perform(post("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "name": "Sales" }
					"""))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		noAuthMvc.perform(get("/api/departments").header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk());
	}

	@Test
	void promotedEmployeesGetHrRightsWithTheirNextToken() throws Exception {
		mockMvc.perform(put("/api/employees/{id}/role", ayse).contentType(MediaType.APPLICATION_JSON).content("""
				{ "role": "HR" }
				""")).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("HR"));

		noAuthMvc.perform(post("/api/departments")
			.header(HttpHeaders.AUTHORIZATION, bearer(token("ayse@example.com", PASSWORD)))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "name": "Sales" }
					"""))
			.andExpect(status().isCreated());
	}

	@Test
	void hrCannotChangeTheirOwnRole() throws Exception {
		long hr = createHrEmployee("Elif", "elif@example.com", engineering);

		mockMvc.perform(put("/api/employees/{id}/role", hr).with(as(hr, Role.HR))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "role": "EMPLOYEE" }
					"""))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void employeesChangeTheirOwnPassword() throws Exception {
		String token = token("ayse@example.com", PASSWORD);

		changePassword(token, "wrong-current", "brand-new-secret").andExpect(status().isUnprocessableContent());
		changePassword(token, PASSWORD, "short").andExpect(status().isBadRequest());
		changePassword(token, PASSWORD, "brand-new-secret").andExpect(status().isNoContent());

		login("ayse@example.com", PASSWORD).andExpect(status().isUnauthorized());
		login("ayse@example.com", "brand-new-secret").andExpect(status().isOk());
	}

	@Test
	void colleaguesDoNotSeeEachOthersBirthDates() throws Exception {
		long colleague = createEmployee("Zeynep", "zeynep@example.com", engineering, null);

		mockMvc.perform(get("/api/employees/{id}", ayse).with(as(colleague)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("ayse@example.com"))
			.andExpect(jsonPath("$.birthDate").doesNotExist());
		mockMvc.perform(get("/api/employees/{id}", ayse).with(as(ayse)))
			.andExpect(jsonPath("$.birthDate").value("1990-05-10"));
		mockMvc.perform(get("/api/employees/{id}", ayse)).andExpect(jsonPath("$.birthDate").value("1990-05-10")); // HR
	}

	@Test
	void documentationAndHealthArePublic() throws Exception {
		noAuthMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		noAuthMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		noAuthMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	private ResultActions login(String email, String password) throws Exception {
		return noAuthMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "%s", "password": "%s" }
				""".formatted(email, password)));
	}

	private String token(String email, String password) throws Exception {
		String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	private ResultActions changePassword(String token, String current, String next) throws Exception {
		return noAuthMvc.perform(put("/api/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "currentPassword": "%s", "newPassword": "%s" }
					""".formatted(current, next)));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

}
