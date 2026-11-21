package com.batuhanturnay.leaveflow.leave;

import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.security.AuthenticatedUser;

/** Data-dependent permission rules for leave, in one place. */
final class LeaveAccess {

	private LeaveAccess() {
	}

	/** The employee, their direct manager, and HR may see an employee's leave and balance. */
	static boolean canView(AuthenticatedUser user, Employee employee) {
		return user.isHr() || user.is(employee.getId()) || isDirectManager(user, employee);
	}

	static boolean isDirectManager(AuthenticatedUser user, Employee employee) {
		return employee.getManager() != null && user.is(employee.getManager().getId());
	}

}
