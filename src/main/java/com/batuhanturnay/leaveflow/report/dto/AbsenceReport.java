package com.batuhanturnay.leaveflow.report.dto;

import java.time.LocalDate;
import java.util.List;

import com.batuhanturnay.leaveflow.common.export.Exportable;
import com.batuhanturnay.leaveflow.common.export.Table;

/** Who is away between two dates: approved leave that overlaps the period. */
public record AbsenceReport(LocalDate from, LocalDate to, List<AbsenceRow> rows) implements Exportable {

	@Override
	public String fileName() {
		return "absences-" + from + "-to-" + to;
	}

	@Override
	public Table table() {
		return new Table("Absences " + from + " to " + to,
				List.of("Request ID", "Employee ID", "Employee", "Department", "Leave type", "Start date",
						"End date", "Working days"),
				rows.stream()
					.map(r -> List.<Object>of(r.leaveRequestId(), r.employeeId(), r.employee(), r.department(),
							r.type(), r.startDate(), r.endDate(), r.workingDays()))
					.toList());
	}

}
