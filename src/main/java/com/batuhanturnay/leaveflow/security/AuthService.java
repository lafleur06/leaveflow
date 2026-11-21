package com.batuhanturnay.leaveflow.security;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.common.error.BusinessRuleException;
import com.batuhanturnay.leaveflow.common.error.UnauthorizedException;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.employee.EmployeeService;
import com.batuhanturnay.leaveflow.security.dto.ChangePasswordRequest;
import com.batuhanturnay.leaveflow.security.dto.LoginRequest;
import com.batuhanturnay.leaveflow.security.dto.TokenResponse;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final EmployeeRepository employeeRepository;

	private final EmployeeService employeeService;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	/**
	 * Hash compared against when the email is unknown, so that a wrong email and a wrong
	 * password take the same time to answer and cannot be told apart.
	 */
	private final String dummyHash;

	public AuthService(EmployeeRepository employeeRepository, EmployeeService employeeService,
			PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.employeeRepository = employeeRepository;
		this.employeeService = employeeService;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	/** Same answer for every failure: unknown email, wrong password, no password yet, inactive. */
	public TokenResponse login(LoginRequest request) {
		Optional<Employee> found = employeeRepository.findByEmailIgnoreCase(request.email().strip());
		String hash = found.map(Employee::getPasswordHash).orElse(null);
		boolean passwordMatches = passwordEncoder.matches(request.password(), hash != null ? hash : dummyHash);
		if (found.isEmpty() || hash == null || !passwordMatches || !found.get().isActive()) {
			throw new UnauthorizedException("Invalid email or password");
		}
		return tokenService.issue(found.get());
	}

	@Transactional
	public void changeOwnPassword(long employeeId, ChangePasswordRequest request) {
		Employee employee = employeeService.getEntity(employeeId);
		if (employee.getPasswordHash() == null
				|| !passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
			throw new BusinessRuleException("The current password is incorrect");
		}
		employee.changePasswordHash(passwordEncoder.encode(request.newPassword()));
	}

}
