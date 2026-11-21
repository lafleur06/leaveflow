package com.batuhanturnay.leaveflow.leave.dto;

import java.time.LocalDate;

/**
 * Annual leave balance in working days.
 * {@code availableDays = earnedDays - usedDays - pendingDays}: pending requests are reserved
 * so that an employee cannot book the same days twice while waiting for approval.
 */
public record LeaveBalanceResponse(Long employeeId, int completedServiceYears, int earnedDays, int usedDays,
		int pendingDays, int availableDays, LocalDate nextAccrualDate, int nextAccrualDays) {

}
