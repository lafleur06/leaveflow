package com.batuhanturnay.leaveflow.employee;

/**
 * Stored roles. "Manager" is deliberately not a role: anyone with direct reports acts as
 * their manager, so permissions always follow the real reporting lines and cannot drift
 * from the org chart.
 */
public enum Role {

	/** Regular employee: own profile, own leave, and their team's leave if they manage anyone. */
	EMPLOYEE,

	/** Human resources: manages employees, departments, holidays and accounts; sees all leave. */
	HR

}
