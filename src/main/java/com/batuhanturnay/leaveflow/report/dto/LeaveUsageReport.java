package com.batuhanturnay.leaveflow.report.dto;

import java.util.List;

import com.batuhanturnay.leaveflow.common.export.Exportable;
import com.batuhanturnay.leaveflow.common.export.Table;

/** Approved leave per department and type for leave starting in the given year. */
public record LeaveUsageReport(int year, List<LeaveUsageRow> rows) implements Exportable {

	@Override
	public String fileName() {
		return "leave-usage-" + year;
	}

	@Override
	public Table table() {
		return new Table("Leave usage " + year,
				List.of("Department", "Leave type", "Approved requests", "Employees", "Working days"),
				rows.stream()
					.map(r -> List.<Object>of(r.department(), r.type(), r.requests(), r.employees(), r.workingDays()))
					.toList());
	}

}
