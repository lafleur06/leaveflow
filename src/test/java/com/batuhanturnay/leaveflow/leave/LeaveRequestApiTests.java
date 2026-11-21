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
import com.batuhanturnay.leaveflow.employee.Role;

/**
 * "Today" is Monday 15 June 2026 (see {@link IntegrationTest#TODAY}).
 * Ayse reports to Mehmet. She was hired on 2023-09-01, so she has completed 2 service years
 * and earned 2 x 14 = 28 days.
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
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(ayse)))
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
	void balanceIsVisibleToTheEmployeeTheirManagerAndHrOnly() throws Exception {
		long colleague = createEmployee("Zeynep", "zeynep@example.com", engineering, manager);

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(manager))).andExpect(status().isOk());
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse)).andExpect(status().isOk()); // HR
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(colleague)))
			.andExpect(status().isForbidden());
	}

	@Test
	void countsWorkingDaysWithoutWeekendsAndHolidaysAndReservesThem() throws Exception {
		// Mon 13 - Fri 17 July 2026; Wednesday 15 July is a public holiday
		submit(ayse, "ANNUAL", "2026-07-13", "2026-07-17").andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.workingDays").value(4))
			.andExpect(jsonPath("$.employee.id").value(ayse))
			.andExpect(jsonPath("$.employee.name").value("Ayse Test"));

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(ayse)))
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

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(ayse)))
			.andExpect(jsonPath("$.usedDays").value(4))
			.andExpect(jsonPath("$.pendingDays").value(0))
			.andExpect(jsonPath("$.availableDays").value(24));
	}

	@Test
	void approvalNeedsNoBody() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		mockMvc.perform(post("/api/leave-requests/{id}/approve", id).with(as(manager)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void onlyTheDirectManagerMayDecide() throws Exception {
		long colleague = createEmployee("Zeynep", "zeynep@example.com", engineering, manager);
		long hr = createHrEmployee("Elif", "elif@example.com", engineering);
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		decide(id, "approve", colleague, null).andExpect(status().isForbidden());
		// HR cannot overrule a manager either
		decideAs(id, "approve", hr, Role.HR, null).andExpect(status().isForbidden());
		decide(id, "approve", ayse, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Employees cannot decide on their own leave requests"));
	}

	@Test
	void hrDecidesForEmployeesWithoutAManager() throws Exception {
		long hr = createHrEmployee("Elif", "elif@example.com", engineering);
		long id = submitAndGetId(manager, "UNPAID", "2026-07-01", "2026-07-01");

		decide(id, "approve", ayse, null).andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("Requests of employees without a manager are decided by HR"));
		decideAs(id, "approve", hr, Role.HR, null).andExpect(status().isOk())
			.andExpect(jsonPath("$.decidedBy.id").value(hr));
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

		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(ayse)))
			.andExpect(jsonPath("$.availableDays").value(28));
	}

	@Test
	void approvedLeaveCanBeCancelledBeforeItStarts() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");
		decide(id, "approve", manager, null).andExpect(status().isOk());

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id).with(as(ayse)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));
		mockMvc.perform(get("/api/employees/{id}/leave-balance", ayse).with(as(ayse)))
			.andExpect(jsonPath("$.usedDays").value(0));
	}

	@Test
	void onlyTheEmployeeOrHrMayCancel() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id).with(as(manager)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id)).andExpect(status().isOk()); // HR
	}

	@Test
	void leaveThatHasStartedCannotBeCancelled() throws Exception {
		long id = submitAndGetId(ayse, "SICK", "2026-06-10", "2026-06-11");
		decide(id, "approve", manager, null).andExpect(status().isOk());

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id).with(as(ayse)))
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

		mockMvc.perform(get("/api/leave-requests").with(as(manager))
			.param("managerId", String.valueOf(manager))
			.param("status", "PENDING"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].employee.id").value(ayse))
			.andExpect(jsonPath("$.content[0].type").value("ANNUAL"));
	}

	@Test
	void employeesSeeOnlyTheirOwnRequestsWhileHrSeesAll() throws Exception {
		long colleague = createEmployee("Zeynep", "zeynep@example.com", engineering, manager);
		long ayseRequest = submitAndGetId(ayse, "ANNUAL", "2026-07-13", "2026-07-17");
		submitAndGetId(colleague, "UNPAID", "2026-07-20", "2026-07-20");

		mockMvc.perform(get("/api/leave-requests").with(as(colleague)))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].employee.id").value(colleague));
		// asking for someone else's requests explicitly does not help
		mockMvc.perform(get("/api/leave-requests").with(as(colleague)).param("employeeId", String.valueOf(ayse)))
			.andExpect(jsonPath("$.content", hasSize(0)));
		mockMvc.perform(get("/api/leave-requests/{id}", ayseRequest).with(as(colleague)))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/leave-requests").with(as(manager))).andExpect(jsonPath("$.content", hasSize(2)));
		mockMvc.perform(get("/api/leave-requests")).andExpect(jsonPath("$.content", hasSize(2))); // HR
	}

	private ResultActions submit(long employeeId, String type, String start, String end) throws Exception {
		return mockMvc.perform(post("/api/leave-requests").with(as(employeeId))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "type": "%s", "startDate": "%s", "endDate": "%s", "reason": "Test" }
					""".formatted(type, start, end)));
	}

	private long submitAndGetId(long employeeId, String type, String start, String end) throws Exception {
		return postAndReturnId("/api/leave-requests", """
				{ "type": "%s", "startDate": "%s", "endDate": "%s" }
				""".formatted(type, start, end), as(employeeId));
	}

	private ResultActions decide(long requestId, String action, long deciderId, String note) throws Exception {
		return decideAs(requestId, action, deciderId, Role.EMPLOYEE, note);
	}

	private ResultActions decideAs(long requestId, String action, long deciderId, Role role, String note)
			throws Exception {
		String noteJson = note == null ? "null" : "\"" + note + "\"";
		return mockMvc.perform(post("/api/leave-requests/{id}/{action}", requestId, action)
			.with(as(deciderId, role))
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "note": %s }
					""".formatted(noteJson)));
	}

	@Test
	void rejectedOrCancelledRequestsCannotBeCancelledAgain() throws Exception {
		long rejected = submitAndGetId(ayse, "ANNUAL", "2026-07-20", "2026-07-24");
		decide(rejected, "reject", manager, "Busy week").andExpect(status().isOk());
		long cancelled = submitAndGetId(ayse, "ANNUAL", "2026-08-03", "2026-08-04");
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", cancelled).with(as(ayse))).andExpect(status().isOk());

		mockMvc.perform(post("/api/leave-requests/{id}/cancel", rejected).with(as(ayse)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("is REJECTED")));
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", cancelled).with(as(ayse)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("is CANCELLED")));
	}

	@Test
	void cancelledRequestCannotBeApproved() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-20", "2026-07-24");
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id).with(as(ayse))).andExpect(status().isOk());

		decide(id, "approve", manager, null).andExpect(status().isConflict());
	}

	@Test
	void leaveCannotStartBeforeTheHireDate() throws Exception {
		long newcomer = createEmployee("Deniz", "deniz@example.com", engineering, manager, "1995-01-01",
				"2026-06-01");

		submit(newcomer, "SICK", "2026-05-28", "2026-06-02").andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("hire date")));
	}

	@Test
	void ownerAndDirectManagerCanOpenARequest() throws Exception {
		long id = submitAndGetId(ayse, "ANNUAL", "2026-07-20", "2026-07-24");

		mockMvc.perform(get("/api/leave-requests/{id}", id).with(as(ayse)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.workingDays").value(5))
			.andExpect(jsonPath("$.status").value("PENDING"));
		mockMvc.perform(get("/api/leave-requests/{id}", id).with(as(manager))).andExpect(status().isOk());
	}

	@Test
	void unknownRequestIsNotFound() throws Exception {
		mockMvc.perform(get("/api/leave-requests/{id}", 424242)).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/leave-requests/{id}/approve", 424242)).andExpect(status().isNotFound());
	}

	@Test
	void filtersByTypeAndStatus() throws Exception {
		submitAndGetId(ayse, "ANNUAL", "2026-07-20", "2026-07-24");
		long sick = submitAndGetId(ayse, "SICK", "2026-06-10", "2026-06-11");
		decide(sick, "approve", manager, null).andExpect(status().isOk());

		mockMvc.perform(get("/api/leave-requests").param("type", "SICK"))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].id").value(sick));
		mockMvc.perform(get("/api/leave-requests").param("status", "PENDING").param("type", "SICK"))
			.andExpect(jsonPath("$.content", hasSize(0)));
	}

	@Test
	void theWholeBalanceCanBeBookedButNotOneDayMore() throws Exception {
		// 6 Jul - 13 Aug 2026 has 29 weekdays; minus 15 July (Democracy and National Unity Day) that is 28
		submit(ayse, "ANNUAL", "2026-07-06", "2026-08-13").andExpect(status().isCreated())
			.andExpect(jsonPath("$.workingDays").value(28));
		submit(ayse, "ANNUAL", "2026-08-17", "2026-08-17").andExpect(status().isUnprocessableContent());
	}
}
