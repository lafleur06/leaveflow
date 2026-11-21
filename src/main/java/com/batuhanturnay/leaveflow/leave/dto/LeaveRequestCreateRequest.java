package com.batuhanturnay.leaveflow.leave.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.batuhanturnay.leaveflow.leave.LeaveType;

/** Body for submitting a leave request. Both dates are inclusive. */
public record LeaveRequestCreateRequest(

		@NotNull Long employeeId,

		@NotNull LeaveType type,

		@NotNull LocalDate startDate,

		@NotNull LocalDate endDate,

		@Size(max = 500) String reason) {

}
