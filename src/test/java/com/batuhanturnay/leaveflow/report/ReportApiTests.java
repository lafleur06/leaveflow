package com.batuhanturnay.leaveflow.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.batuhanturnay.leaveflow.IntegrationTest;

/**
 * Ayse (Engineering, 28 days earned) has 5 approved and 2 pending annual days, plus 2 days of
 * approved sick leave. Mehmet is her manager; Elif works in Sales.
 */
class ReportApiTests extends IntegrationTest {

	private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private long engineering;

	private long sales;

	private long manager;

	private long ayse;

	private long elif;

	@BeforeEach
	void setUp() throws Exception {
		engineering = createDepartment("Engineering");
		sales = createDepartment("Sales");
		manager = createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		ayse = createEmployee("Ayşe", "ayse@example.com", engineering, manager, "1998-07-11", "2023-09-01");
		elif = createEmployee("Elif", "elif@example.com", sales, null);

		approve(submit(ayse, "ANNUAL", "2026-07-20", "2026-07-24"));
		submit(ayse, "ANNUAL", "2026-08-03", "2026-08-04");
		approve(submit(ayse, "SICK", "2026-06-10", "2026-06-11"));
	}

	@Test
	void balancesMatchTheSingleEmployeeEndpoint() throws Exception {
		mockMvc.perform(get("/api/reports/leave-balances"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.asOf").value(TODAY.toString()))
			.andExpect(jsonPath("$.rows", hasSize(3)))
			// ordered by department, then name: Engineering (Ayşe, Mehmet), Sales (Elif)
			.andExpect(jsonPath("$.rows[0].employee").value("Ayşe Test"))
			.andExpect(jsonPath("$.rows[0].manager").value("Mehmet Test"))
			.andExpect(jsonPath("$.rows[0].earnedDays").value(28))
			.andExpect(jsonPath("$.rows[0].usedDays").value(5))
			.andExpect(jsonPath("$.rows[0].pendingDays").value(2))
			.andExpect(jsonPath("$.rows[0].availableDays").value(21))
			.andExpect(jsonPath("$.rows[2].department").value("Sales"));

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(jsonPath("$.availableDays").value(21))
			.andExpect(jsonPath("$.usedDays").value(5));
	}

	@Test
	void balancesCanBeLimitedToADepartmentAndSkipFormerEmployees() throws Exception {
		mockMvc.perform(get("/api/reports/leave-balances").param("departmentId", String.valueOf(sales)))
			.andExpect(jsonPath("$.rows", hasSize(1)))
			.andExpect(jsonPath("$.rows[0].employeeId").value(elif));

		mockMvc.perform(delete("/api/employees/{id}", elif)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/reports/leave-balances").param("departmentId", String.valueOf(sales)))
			.andExpect(jsonPath("$.rows", hasSize(0)));
	}

	@Test
	void downloadsBalancesAsCsv() throws Exception {
		byte[] body = mockMvc.perform(get("/api/reports/leave-balances").accept("text/csv"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
			.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
					startsWith("attachment; filename=\"leave-balances-2026-06-15.csv\"")))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		String csv = new String(body, StandardCharsets.UTF_8);
		assertThat(csv).startsWith("﻿Employee ID,Employee,Email,Department,Manager,Hire date,");
		String[] lines = csv.split("\r\n");
		assertThat(lines).hasSize(4);
		assertThat(lines[1]).isEqualTo(
				ayse + ",Ayşe Test,ayse@example.com,Engineering,Mehmet Test,2023-09-01,2,28,5,2,21");
	}

	@Test
	void formatParameterWorksLikeTheAcceptHeader() throws Exception {
		mockMvc.perform(get("/api/reports/leave-usage").param("format", "csv"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename=\"leave-usage-2026.csv\"")));
		mockMvc.perform(get("/api/reports/leave-usage").accept(MediaType.ALL))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void downloadsBalancesAsExcelWithRealNumbersAndDates() throws Exception {
		byte[] body = mockMvc.perform(get("/api/reports/leave-balances").accept(XLSX))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(XLSX))
			.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename=\"leave-balances-2026-06-15.xlsx\"")))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body))) {
			Sheet sheet = workbook.getSheetAt(0);
			assertThat(sheet.getSheetName()).isEqualTo("Leave balances 2026-06-15");
			assertThat(sheet.getLastRowNum()).isEqualTo(3);
			assertThat(sheet.getRow(0).getCell(10).getStringCellValue()).isEqualTo("Available days");
			Row first = sheet.getRow(1);
			assertThat(first.getCell(1).getStringCellValue()).isEqualTo("Ayşe Test");
			assertThat(first.getCell(5).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2023, 9, 1));
			assertThat(first.getCell(10).getNumericCellValue()).isEqualTo(21);
		}
	}

	@Test
	void usageCountsApprovedLeaveStartingInTheYear() throws Exception {
		mockMvc.perform(get("/api/reports/leave-usage").param("year", "2026"))
			.andExpect(jsonPath("$.year").value(2026))
			.andExpect(jsonPath("$.rows", hasSize(2)))
			.andExpect(jsonPath("$.rows[0].department").value("Engineering"))
			.andExpect(jsonPath("$.rows[0].type").value("ANNUAL"))
			.andExpect(jsonPath("$.rows[0].requests").value(1)) // the pending request is not counted
			.andExpect(jsonPath("$.rows[0].employees").value(1))
			.andExpect(jsonPath("$.rows[0].workingDays").value(5))
			.andExpect(jsonPath("$.rows[1].type").value("SICK"))
			.andExpect(jsonPath("$.rows[1].workingDays").value(2));

		mockMvc.perform(get("/api/reports/leave-usage").param("year", "2027")).andExpect(jsonPath("$.rows", hasSize(0)));
		mockMvc.perform(get("/api/reports/leave-usage").param("year", "1999")).andExpect(status().isBadRequest());
	}

	@Test
	void absencesListApprovedLeaveOverlappingThePeriod() throws Exception {
		mockMvc.perform(get("/api/reports/absences").param("from", "2026-06-11").param("to", "2026-07-20"))
			.andExpect(jsonPath("$.rows", hasSize(2)))
			.andExpect(jsonPath("$.rows[0].type").value("SICK"))
			.andExpect(jsonPath("$.rows[1].type").value("ANNUAL"))
			.andExpect(jsonPath("$.rows[1].startDate").value("2026-07-20"));

		// default: today and the next 30 days, which holds neither request
		mockMvc.perform(get("/api/reports/absences"))
			.andExpect(jsonPath("$.from").value("2026-06-15"))
			.andExpect(jsonPath("$.to").value("2026-07-15"))
			.andExpect(jsonPath("$.rows", hasSize(0)));
	}

	@Test
	void absencePeriodMustBeValid() throws Exception {
		mockMvc.perform(get("/api/reports/absences").param("from", "2026-07-01").param("to", "2026-06-01"))
			.andExpect(status().isUnprocessableContent());
		mockMvc.perform(get("/api/reports/absences").param("from", "2026-01-01").param("to", "2027-01-02"))
			.andExpect(status().isUnprocessableContent());
		mockMvc.perform(get("/api/reports/absences").param("from", "not-a-date")).andExpect(status().isBadRequest());
	}

	@Test
	void onlyHrSeesReportsWhateverTheFormat() throws Exception {
		mockMvc.perform(get("/api/reports/leave-balances").with(as(manager))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/reports/leave-balances").with(as(ayse)).accept("text/csv"))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/reports/absences").with(as(ayse)).param("format", "xlsx"))
			.andExpect(status().isForbidden());
		noAuthMvc.perform(get("/api/reports/leave-usage")).andExpect(status().isUnauthorized());
	}

	private long submit(long employeeId, String type, String start, String end) throws Exception {
		return postAndReturnId("/api/leave-requests", """
				{ "type": "%s", "startDate": "%s", "endDate": "%s" }
				""".formatted(type, start, end), as(employeeId));
	}

	private void approve(long requestId) throws Exception {
		mockMvc.perform(post("/api/leave-requests/{id}/approve", requestId).with(as(manager)))
			.andExpect(status().isOk());
	}

}
