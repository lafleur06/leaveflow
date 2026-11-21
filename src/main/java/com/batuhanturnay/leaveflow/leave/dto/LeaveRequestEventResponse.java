package com.batuhanturnay.leaveflow.leave.dto;

import java.time.Instant;

import com.batuhanturnay.leaveflow.common.web.Ref;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeResponse;
import com.batuhanturnay.leaveflow.leave.LeaveAction;
import com.batuhanturnay.leaveflow.leave.LeaveRequestEvent;

/**
 * One step in a leave request's history.
 *
 * @param actor who did it; {@code null} only for cancellations recorded before the audit trail existed
 */
public record LeaveRequestEventResponse(LeaveAction action, Ref actor, String note, Instant occurredAt) {

	public static LeaveRequestEventResponse from(LeaveRequestEvent event) {
		return new LeaveRequestEventResponse(event.getAction(), EmployeeResponse.refTo(event.getActor()),
				event.getNote(), event.getOccurredAt());
	}

}
