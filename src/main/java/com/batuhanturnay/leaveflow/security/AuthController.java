package com.batuhanturnay.leaveflow.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.batuhanturnay.leaveflow.employee.EmployeeService;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeResponse;
import com.batuhanturnay.leaveflow.security.dto.ChangePasswordRequest;
import com.batuhanturnay.leaveflow.security.dto.LoginRequest;
import com.batuhanturnay.leaveflow.security.dto.TokenResponse;

@Tag(name = "Authentication", description = "Log in and manage your own account")
@RestController
public class AuthController {

	private final AuthService authService;

	private final EmployeeService employeeService;

	private final CurrentUser currentUser;

	public AuthController(AuthService authService, EmployeeService employeeService, CurrentUser currentUser) {
		this.authService = authService;
		this.employeeService = employeeService;
		this.currentUser = currentUser;
	}

	@Operation(summary = "Log in with email and password and receive an access token")
	@SecurityRequirements // no token needed
	@PostMapping("/api/auth/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@Operation(summary = "Your own employee profile")
	@GetMapping("/api/me")
	public EmployeeResponse me() {
		return employeeService.findById(currentUser.get().id());
	}

	@Operation(summary = "Change your own password")
	@PutMapping("/api/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		authService.changeOwnPassword(currentUser.get().id(), request);
	}

}
