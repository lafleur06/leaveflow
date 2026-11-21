package com.batuhanturnay.leaveflow.report.dto;

import java.time.LocalDate;

/** One active employee's annual leave balance, in working days. */
public record LeaveBalanceRow(Long employeeId, String employee, String email, String department, String manager,
		LocalDate hireDate, int completedServiceYears, int earnedDays, int usedDays, int pendingDays,
		int availableDays) {

}
