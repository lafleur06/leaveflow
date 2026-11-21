package com.batuhanturnay.leaveflow.demo;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the login page offer one-click demo logins. Exists only with the {@code demo} profile. */
@Tag(name = "Demo", description = "Only in the public demo")
@RestController
@Profile("demo")
@RequestMapping("/api/demo")
class DemoController {

	private static final Map<String, String> PERSONAS = Map.of(
			DemoDataInitializer.EMPLOYEE_EMAIL, "Employee: books leave and sees her balance",
			DemoDataInitializer.MANAGER_EMAIL, "Manager: approves the engineering team's requests",
			DemoDataInitializer.HR_EMAIL, "HR: manages staff and downloads reports");

	private static final Map<String, String> NAMES = Map.of(DemoDataInitializer.EMPLOYEE_EMAIL, "Ayşe Yılmaz",
			DemoDataInitializer.MANAGER_EMAIL, "Mehmet Kaya", DemoDataInitializer.HR_EMAIL, "Zeynep Arslan");

	private final DemoProperties properties;

	DemoController(DemoProperties properties) {
		this.properties = properties;
	}

	@Operation(summary = "The demo accounts and their shared password")
	@SecurityRequirements
	@GetMapping("/accounts")
	List<DemoAccount> accounts() {
		return DemoDataInitializer.LOGIN_ACCOUNTS.stream()
			.map(email -> new DemoAccount(NAMES.get(email), email, properties.password(), PERSONAS.get(email)))
			.toList();
	}

}
