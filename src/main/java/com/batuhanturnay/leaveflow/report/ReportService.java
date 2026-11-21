package com.batuhanturnay.leaveflow.report;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.common.error.BusinessRuleException;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.leave.LeaveBalanceService;
import com.batuhanturnay.leaveflow.leave.dto.LeaveBalanceResponse;
import com.batuhanturnay.leaveflow.report.dto.AbsenceReport;
import com.batuhanturnay.leaveflow.report.dto.AbsenceRow;
import com.batuhanturnay.leaveflow.report.dto.LeaveBalanceReport;
import com.batuhanturnay.leaveflow.report.dto.LeaveBalanceRow;
import com.batuhanturnay.leaveflow.report.dto.LeaveUsageReport;

@Service
@Transactional(readOnly = true)
public class ReportService {

	/** Keeps a single absence report to a manageable size (and a bounded query). */
	static final int MAX_ABSENCE_PERIOD_DAYS = 366;

	private final LeaveReportQueries queries;

	private final Clock clock;

	ReportService(LeaveReportQueries queries, Clock clock) {
		this.queries = queries;
		this.clock = clock;
	}

	/** Balances as of today, computed with the same rule as {@code GET /api/employees/{id}/leave-balance}. */
	public LeaveBalanceReport balances(Long departmentId) {
		LocalDate today = LocalDate.now(clock);
		List<Employee> employees = queries.activeEmployees(departmentId);
		Map<Long, LeaveReportQueries.AnnualDays> days = queries.annualDaysByEmployee(departmentId);
		List<LeaveBalanceRow> rows = employees.stream().map(employee -> {
			LeaveReportQueries.AnnualDays annual = days.getOrDefault(employee.getId(),
					LeaveReportQueries.AnnualDays.NONE);
			LeaveBalanceResponse balance = LeaveBalanceService.balanceOf(employee, annual.used(), annual.pending(),
					today);
			return new LeaveBalanceRow(employee.getId(), employee.getFullName(), employee.getEmail(),
					employee.getDepartment().getName(),
					employee.getManager() == null ? null : employee.getManager().getFullName(),
					employee.getHireDate(), balance.completedServiceYears(), balance.earnedDays(), balance.usedDays(),
					balance.pendingDays(), balance.availableDays());
		}).toList();
		return new LeaveBalanceReport(today, rows);
	}

	/** Approved leave starting in {@code year}; the current year when {@code null}. */
	public LeaveUsageReport usage(Integer year) {
		int reportYear = year == null ? LocalDate.now(clock).getYear() : year;
		return new LeaveUsageReport(reportYear,
				queries.usage(LocalDate.of(reportYear, 1, 1), LocalDate.of(reportYear, 12, 31)));
	}

	/** Approved leave overlapping the period; today plus the next 30 days when no dates are given. */
	public AbsenceReport absences(LocalDate from, LocalDate to) {
		LocalDate start = from == null ? LocalDate.now(clock) : from;
		LocalDate end = to == null ? start.plusDays(30) : to;
		if (end.isBefore(start)) {
			throw new BusinessRuleException("to must not be before from");
		}
		if (ChronoUnit.DAYS.between(start, end) >= MAX_ABSENCE_PERIOD_DAYS) {
			throw new BusinessRuleException(
					"The period may be at most %d days long".formatted(MAX_ABSENCE_PERIOD_DAYS));
		}
		List<AbsenceRow> rows = queries.approvedLeaveOverlapping(start, end)
			.stream()
			.map(r -> new AbsenceRow(r.getId(), r.getEmployee().getId(), r.getEmployee().getFullName(),
					r.getEmployee().getDepartment().getName(), r.getType(), r.getStartDate(), r.getEndDate(),
					r.getWorkingDays()))
			.toList();
		return new AbsenceReport(start, end, rows);
	}

}
