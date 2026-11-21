package com.batuhanturnay.leaveflow.report.dto;

import java.time.LocalDate;

import com.batuhanturnay.leaveflow.leave.LeaveType;

/** One approved absence. {@code workingDays} covers the whole request, not only the report period. */
public record AbsenceRow(Long leaveRequestId, Long employeeId, String employee, String department, LeaveType type,
		LocalDate startDate, LocalDate endDate, int workingDays) {

}
