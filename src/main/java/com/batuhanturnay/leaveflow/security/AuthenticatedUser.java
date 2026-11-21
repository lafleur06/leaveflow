package com.batuhanturnay.leaveflow.security;

import com.batuhanturnay.leaveflow.employee.Role;

/** Who is calling, as stated by the verified access token. */
public record AuthenticatedUser(long id, Role role) {

	public boolean isHr() {
		return role == Role.HR;
	}

	public boolean is(Long employeeId) {
		return employeeId != null && employeeId == id;
	}

}
