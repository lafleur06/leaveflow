package com.batuhanturnay.leaveflow.leave.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional for approval, required (with a note) for rejection.
 * The approver is always the logged-in user.
 */
public record LeaveDecisionRequest(@Size(max = 500) String note) {

	public static final LeaveDecisionRequest EMPTY = new LeaveDecisionRequest(null);

}
