package com.batuhanturnay.leaveflow.leave;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.batuhanturnay.leaveflow.IntegrationTest;

/**
 * "Today" is Monday 15 June 2026 (see {@link IntegrationTest#TODAY}).
 * Ayse was hired on 2023-09-01, so she has completed 2 service years and earned 2 x 14 = 28 days.
 */
class LeaveRequestApiTests extends IntegrationTest {

	private long engineering;

	private long manager;

	private long ayse;

	@BeforeEach
	void setUp() throws Exception {
		engineering = createDepartment("Engineering");
		manager = createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		ayse = createEmployee("Ayse", "ayse@example.com", engineering, manager, "1998-07-11", "2023-09-01");
	}

	@Test
	void calculatesBalanceFromServiceYears() throws Exception {
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.completedServiceYears").value(2))
			.andExpect(jsonPath("$.earnedDays").value(28))
			.andExpect(jsonPath("$.usedDays").value(0))
			.andExpect(jsonPath("$.pendingDays").value(0))
			.andExpect(jsonPath("$.availableDays").value(28))
			.andExpect(jsonPath("$.nextAccrualDate").value("2026-09-01"))
			.andExpect(jsonPath("$.nextAccrualDays").value(14));
	}

	@Test
	void countsWorkingDaysWithoutWeekendsAndHolidaysAndReservesThem() throws Exception {
		// Mon 13 - Fri 17 July 2026; Wednesday 15 July is a public holiday
		submit(ayse, "ANNUAL", "2026-07-13", "2026-07-17").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.workingDays").value(4))
			.andExpect(jsonPath("$.employee.name").value("Ayse Test"));

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(jsonPath("$.pendingDays").value(4))
			.andExpect(jsonPath("$.availableDays").value(24));
	}

	@Test
	void rejectsPeriodWithoutWorkingDays() throws Exception {
		submit(ayse, "ANNUAL", "2026-06-20", "2026-06-21").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("no working days")));
	}

	@Test
	void rejectsEndBeforeStart() throws Exception {
		submit(ayse, "ANNUAL", "2026-07-17", "2026-07-13").andExpect(status().isUnprocessableContent());
	}

	@Test
	void rejectsOverlappingRequests() throws Exception {
		submit(ayse, "ANNUAL", "2026-07-13", "2026-07-17").andExpect(status().isCreated());

		submit(ayse, "UNPAID", "2026-07-17", "2026-07-20").andExpect(status().isConflict());
	}

	@Test
	void rejectsAnnualLeaveBeyondTheBalance() throws Exception {
		// 22 June - 14 August 2026: 40 weekdays minus 15 July = 39 working days, only 28 earned
		submit(ayse, "ANNUAL", "2026-06-22", "2026-08-14").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("Insufficient annual leave: 39 working days requested, 28 available"));
	}

	@Test
	void pendingRequestsCountAgainstTheBalance() throws Exception {
		submit(ayse, "ANNUAL", "2026-06-22", "2026-07-24").andExpect(status().isCreated())
			.andExpect(jsonPath("$.workingDays").value(24));

		// 4 left; this asks for 5
		submit(ayse, "ANNUAL", "2026-08-03", "2026-08-07").andExpect(status().isUnprocessableContent());
	}

	@Test
	void newHiresHaveNoAnnualLeaveButMayTakeUnpaidLeave() throws Exception {
		long newHire = createEmployee("Can", "can@example.com", engineering, manager, "2000-02-02", "2026-01-05");

		submit(newHire, "ANNUAL", "2026-07-01", "2026-07-02").andExpect(status().isUnprocessableContent());
		submit(newHire, "UNPAID", "2026-07-01", "2026-07-02").andExpect(status().isCreated());
	}

	@Test
	void annualLeaveCannotStartInThePastButSickLeaveCan() throws Exception {
		submit(ayse, "ANNUAL", "2026-06-10", "2026-06-11").andExpect(status().isUnprocessableContent());

		submit(ayse, "SICK", "2026-06-10", "2026-06-11").andExpect(status().isCreated())
			.andExpect(jsonPath("$.workingDays").value(2));
	}

	@Test
	void inactiveEmployeesCannotRequestLeave() throws Exception {
		long former = createEmployee("Former", "former@example.com", engineering, null);
		mockMvc.perform(delete("/api/employees/{id}", former)).andExpect(status().isNoContent());

		submit(former, "UNPAID", "2026-07-01", "2026-07-01").andExpect(status().isUnprocessableContent());
	}

	@Test
	void rejectsUnknownLeaveType() throws Exception {
		submit(ayse, "HOLIDAY", "2026-07-01", "2026-07-01").andExpect(status().isBadRequest());
	}

	@Test
	void directManagerApproves() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		decide(id, "approve", manager, "Enjoy").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"))
			.andExpect(jsonPath("$.decidedBy.id").value(manager))
			.andExpect(jsonPath("$.decisionNote").value("Enjoy"))
			.andExpect(jsonPath("$.decidedAt").isNotEmpty());

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(jsonPath("$.usedDays").value(4))
			.andExpect(jsonPath("$.pendingDays").value(0))
			.andExpect(jsonPath("$.availableDays").value(24));
	}

	@Test
	void onlyTheDirectManagerMayDecide() throws Exception {
		long colleague = createEmployee("Zeynep", "zeynep@example.com", engineering, manager);
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		decide(id, "approve", colleague, null).andExpect(status().isForbidden());
		decide(id, "approve", ayse, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Employees cannot decide on their own leave requests"));
	}

	@Test
	void requestsOfEmployeesWithoutAManagerCannotBeDecidedYet() throws Exception {
		long id = submitAndGetId(manager, "UNPAID", "2026-07-01", "2026-07-01");

		decide(id, "approve", ayse, null).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("has no manager")));
	}

	@Test
	void aRequestCanOnlyBeDecidedOnce() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");
		decide(id, "approve", manager, null).andExpect(status().isOk());

		decide(id, "reject", manager, "Changed my mind").andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("already APPROVED")));
	}

	@Test
	void rejectionNeedsANoteAndReleasesTheDays() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		decide(id, "reject", manager, " ").andExpect(status().isUnprocessableContent());
		decide(id, "reject", manager, "Release week, please pick other dates").andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("REJECTED"));

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(jsonPath("$.availableDays").value(28));
	}

	@Test
	void approvedLeaveCanBeCancelledBeforeItStarts() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");
		decide(id, "approve", manager, null).andExpect(status().isOk());

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse))
			.andExpect(jsonPath("$.usedDays").value(0));
	}

	@Test
	void leaveThatHasStartedCannotBeCancelled() throws Exception {
		long id = submitAndGetId(ayse, "SICK", "2026-06-10", "2026-06-11");
		decide(id, "approve", manager, null).andExpect(status().isOk());

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("already started")));
	}

	@Test
	void managerSeesTheApprovalQueueOfTheirTeamOnly() throws Exception {
		long otherManager = createEmployee("Elif", "elif@example.com", engineering, null);
		long otherTeam = createEmployee("Burak", "burak@example.com", engineering, otherManager);
		submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");
		long decided = submitAndGetId(ayse, "UNPAID", "2026-08-03", "2026-08-03");
		decide(decided, "approve", manager, null).andExpect(status().isOk());
		submitAndGetId(otherTeam, "UNPAID", "2026-07-13", "2026-07-13");

		mockMvc.perform(get("/api/leave-requests").param("managerId", String.valueOf(manager))
			.param("status", "PENDING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].employee.id").value(ayse))
			.andExpect(jsonPath("$.content[0].type").value("ANNUAL"));
	}

	private ResultActions submit(long employeeId, String type, String start, String end) throws Exception {
		return mockMvc.perform(post("/api/leave-requests").contentType(MediaType.APPLICATION_JSON).content("""
				{ "employeeId": %d, "type": "%s", "startDate": "%s", "endDate": "%s", "reason": "Test" }
				""".formatted(employeeId, type, start, end)));
	}

	private long submitAndGetId(long employeeId, String type, String start, String end) throws Exception {
		return postAndReturnId("/api/leave-requests", """
				{ "employeeId": %d, "type": "%s", "startDate": "%s", "endDate": "%s" }
				""".formatted(employeeId, type, start, end));
	}

	private ResultActions decide(long requestId, String action, long approverId, String note) throws Exception {
		String noteJson = note == null ? "null" : "\"" + note + "\"";
		return mockMvc.perform(post("/api/leave-requests/{id}/{action}", requestId, action)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "approverId": %d, "note": %s }
					""".formatted(approverId, noteJson)));
	}

}
