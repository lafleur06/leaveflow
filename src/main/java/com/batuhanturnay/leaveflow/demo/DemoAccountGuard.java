package com.batuhanturnay.leaveflow.demo;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.batuhanturnay.leaveflow.common.error.ForbiddenException;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.security.CurrentUser;

/**
 * Keeps the public demo usable for the next visitor: the accounts offered on the login page
 * cannot be edited, deactivated, have their role changed or their password changed.
 * Everything else works, including creating new employees and changing those.
 */
@Component
@Profile("demo")
class DemoAccountGuard implements HandlerInterceptor, WebMvcConfigurer {

	private final EmployeeRepository employees;

	private final CurrentUser currentUser;

	DemoAccountGuard(EmployeeRepository employees, CurrentUser currentUser) {
		this.employees = employees;
		this.currentUser = currentUser;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(this).addPathPatterns("/api/me/password", "/api/employees/*", "/api/employees/*/*");
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.POST.matches(request.getMethod())) {
			return true; // reading, and creating new employees, is always fine
		}
		Long target = targetEmployee(request.getRequestURI().substring(request.getContextPath().length()));
		if (target != null && isLoginAccount(target)) {
			throw new ForbiddenException(
					"The demo accounts cannot be changed. Create a new employee to try this out.");
		}
		return true;
	}

	/** The employee a write request changes: yourself for /api/me/password, or the id in the path. */
	private Long targetEmployee(String path) {
		if (path.equals("/api/me/password")) {
			return currentUser.get().id();
		}
		String[] segments = path.split("/"); // "", "api", "employees", "{id}", ...
		if (segments.length >= 4 && segments[3].chars().allMatch(Character::isDigit) && !segments[3].isEmpty()) {
			return Long.valueOf(segments[3]);
		}
		return null;
	}

	private boolean isLoginAccount(long employeeId) {
		return employees.findById(employeeId)
			.map(employee -> DemoDataInitializer.LOGIN_ACCOUNTS.contains(employee.getEmail().toLowerCase(Locale.ROOT)))
			.orElse(false);
	}

}
