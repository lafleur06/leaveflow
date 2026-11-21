package com.batuhanturnay.leaveflow.report.dto;

import com.batuhanturnay.leaveflow.leave.LeaveType;

/**
 * Approved leave of one type in one department.
 *
 * @param requests number of approved requests
 * @param employees how many different employees took this kind of leave
 * @param workingDays total working days taken
 */
public record LeaveUsageRow(String department, LeaveType type, long requests, long employees, long workingDays) {

}
