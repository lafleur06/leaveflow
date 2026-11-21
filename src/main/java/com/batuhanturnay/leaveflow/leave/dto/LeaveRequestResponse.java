package com.batuhanturnay.leaveflow.leave.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.batuhanturnay.leaveflow.common.web.Ref;
import com.batuhanturnay.leaveflow.employee.dto.EmployeeResponse;
import com.batuhanturnay.leaveflow.leave.LeaveRequest;
import com.batuhanturnay.leaveflow.leave.LeaveStatus;
import com.batuhanturnay.leaveflow.leave.LeaveType;

public record LeaveRequestResponse(Long id, Ref employee, LeaveType type, LocalDate startDate, LocalDate endDate,
		int workingDays, String reason, LeaveStatus status, Ref decidedBy, Instant decidedAt, String decisionNote,
		Instant createdAt, Instant updatedAt) {

	public static LeaveRequestResponse from(LeaveRequest r) {
		return new LeaveRequestResponse(r.getId(), EmployeeResponse.refTo(r.getEmployee()), r.getType(),
				r.getStartDate(), r.getEndDate(), r.getWorkingDays(), r.getReason(), r.getStatus(),
				EmployeeResponse.refTo(r.getDecidedBy()), r.getDecidedAt(), r.getDecisionNote(), r.getCreatedAt(),
				r.getUpdatedAt());
	}

}
