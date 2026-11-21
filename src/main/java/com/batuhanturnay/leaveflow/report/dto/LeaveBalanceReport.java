package com.batuhanturnay.leaveflow.report.dto;

import java.time.LocalDate;
import java.util.List;

import com.batuhanturnay.leaveflow.common.export.Exportable;
import com.batuhanturnay.leaveflow.common.export.Table;

/** Annual leave balances of all active employees (or one department) on a given day. */
public record LeaveBalanceReport(LocalDate asOf, List<LeaveBalanceRow> rows) implements Exportable {

	@Override
	public String fileName() {
		return "leave-balances-" + asOf;
	}

	@Override
	public Table table() {
		return new Table("Leave balances " + asOf,
				List.of("Employee ID", "Employee", "Email", "Department", "Manager", "Hire date", "Service years",
						"Earned days", "Used days", "Pending days", "Available days"),
				rows.stream()
					.map(r -> List.<Object>of(r.employeeId(), r.employee(), r.email(), r.department(),
							r.manager() == null ? "" : r.manager(), r.hireDate(), r.completedServiceYears(),
							r.earnedDays(), r.usedDays(), r.pendingDays(), r.availableDays()))
					.toList());
	}

}
