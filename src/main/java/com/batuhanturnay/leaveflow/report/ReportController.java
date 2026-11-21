package com.batuhanturnay.leaveflow.report;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batuhanturnay.leaveflow.common.export.XlsxHttpMessageConverter;
import com.batuhanturnay.leaveflow.report.dto.AbsenceReport;
import com.batuhanturnay.leaveflow.report.dto.LeaveBalanceReport;
import com.batuhanturnay.leaveflow.report.dto.LeaveUsageReport;

/**
 * HR reports. Each one is JSON by default and a download when asked for
 * {@code text/csv} or Excel, through the {@code Accept} header or {@code ?format=csv|xlsx}.
 * JSON is listed first in {@code produces}, so it stays the default for {@code Accept: *}{@code /*}.
 */
@Tag(name = "Reports", description = "HR reports as JSON, CSV or Excel (Accept header or ?format=csv|xlsx)")
@RestController
@RequestMapping(path = "/api/reports",
		produces = { MediaType.APPLICATION_JSON_VALUE, "text/csv", XlsxHttpMessageConverter.XLSX_VALUE })
@PreAuthorize("hasRole('HR')")
public class ReportController {

	private final ReportService reportService;

	ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@Operation(summary = "Annual leave balance of every active employee today")
	@GetMapping("/leave-balances")
	public LeaveBalanceReport balances(
			@Parameter(description = "Only this department") @RequestParam(required = false) Long departmentId) {
		return reportService.balances(departmentId);
	}

	@Operation(summary = "Approved leave per department and type, for leave starting in the given year")
	@GetMapping("/leave-usage")
	public LeaveUsageReport usage(@Parameter(description = "Defaults to the current year") @RequestParam(
			required = false) @Min(2000) @Max(2100) Integer year) {
		return reportService.usage(year);
	}

	@Operation(summary = "Who is away: approved leave overlapping a period (at most a year)")
	@GetMapping("/absences")
	public AbsenceReport absences(
			@Parameter(description = "Defaults to today") @RequestParam(
					required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@Parameter(description = "Defaults to 30 days after from") @RequestParam(
					required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reportService.absences(from, to);
	}

}
