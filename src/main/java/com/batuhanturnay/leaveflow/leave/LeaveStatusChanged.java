package com.batuhanturnay.leaveflow.leave;

import java.time.Instant;

import com.batuhanturnay.leaveflow.employee.Employee;

/**
 * Raised by {@link LeaveRequest} whenever its status changes, and published by Spring Data
 * when the request is saved. {@link LeaveAuditTrail} turns it into a history row.
 *
 * @param actor who caused the change: the employee for a submission, the decider, or whoever cancelled
 */
public record LeaveStatusChanged(LeaveRequest request, LeaveAction action, Employee actor, String note,
		Instant occurredAt) {

}
