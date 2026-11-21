package com.batuhanturnay.leaveflow.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for approving or rejecting a request. A note is optional for approval and required for rejection.
 * Until authentication arrives (Phase 3), the approver identifies themselves with {@code approverId}.
 */
public record LeaveDecisionRequest(

		@NotNull Long approverId,

		@Size(max = 500) String note) {

}
