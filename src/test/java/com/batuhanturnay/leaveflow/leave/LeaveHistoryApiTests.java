package com.batuhanturnay.leaveflow.leave;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.batuhanturnay.leaveflow.IntegrationTest;
import com.batuhanturnay.leaveflow.employee.Role;

/** The audit trail: every status change of a leave request leaves one history entry. */
class LeaveHistoryApiTests extends IntegrationTest {

	/** The fixed clock of {@link IntegrationTest}, as the API writes it. */
	private static final String NOW = "2026-06-15T06:00:00Z";

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
	void recordsSubmissionDecisionAndCancellationWithTheirActors() throws Exception {
		long id = submit();
		decide(id, "approve", "Enjoy").andExpect(status().isOk());
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", id).with(as(ayse))).andExpect(status().isOk());

		mockMvc.perform(get("/api/leave-requests/{id}/history", id).with(as(ayse)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].action").value("SUBMITTED"))
			.andExpect(jsonPath("$[0].actor.id").value(ayse))
			.andExpect(jsonPath("$[0].note").value(nullValue()))
			.andExpect(jsonPath("$[0].occurredAt").value(NOW))
			.andExpect(jsonPath("$[1].action").value("APPROVED"))
			.andExpect(jsonPath("$[1].actor.id").value(manager))
			.andExpect(jsonPath("$[1].actor.name").value("Mehmet Test"))
			.andExpect(jsonPath("$[1].note").value("Enjoy"))
			.andExpect(jsonPath("$[2].action").value("CANCELLED"))
			.andExpect(jsonPath("$[2].actor.id").value(ayse));
	}

	@Test
	void recordsRejectionNoteAndWhichHrUserCancelled() throws Exception {
		long rejected = submit();
		decide(rejected, "reject", "Release week").andExpect(status().isOk());
		long cancelled = postAndReturnId("/api/leave-requests", """
				{ "type": "UNPAID", "startDate": "2026-08-03", "endDate": "2026-08-04" }
				""", as(ayse));
		long hr = createHrEmployee("Selin", "selin@example.com", engineering);
		mockMvc.perform(post("/api/leave-requests/{id}/cancel", cancelled).with(as(hr, Role.HR)))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/leave-requests/{id}/history", rejected).with(as(manager)))
			.andExpect(jsonPath("$[1].action").value("REJECTED"))
			.andExpect(jsonPath("$[1].note").value("Release week"));
		mockMvc.perform(get("/api/leave-requests/{id}/history", cancelled))
			.andExpect(jsonPath("$[1].action").value("CANCELLED"))
			.andExpect(jsonPath("$[1].actor.id").value(hr));
	}

	@Test
	void refusedChangesLeaveNoTrace() throws Exception {
		long id = submit();
		decide(id, "approve", null).andExpect(status().isOk());

		decide(id, "reject", "Changed my mind").andExpect(status().isConflict());
		mockMvc.perform(post("/api/leave-requests/{id}/approve", id).with(as(ayse))).andExpect(status().isForbidden());

		mockMvc.perform(get("/api/leave-requests/{id}/history", id))
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[1].action").value("APPROVED"));
	}

	@Test
	void historyIsVisibleToTheSamePeopleAsTheRequest() throws Exception {
		long id = submit();
		long colleague = createEmployee("Can", "can@example.com", engineering, manager);

		mockMvc.perform(get("/api/leave-requests/{id}/history", id).with(as(colleague)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/leave-requests/{id}/history", id).with(as(manager))).andExpect(status().isOk());
		mockMvc.perform(get("/api/leave-requests/{id}/history", 424242)).andExpect(status().isNotFound());
	}

	private long submit() throws Exception {
		return postAndReturnId("/api/leave-requests", """
				{ "type": "ANNUAL", "startDate": "2026-07-20", "endDate": "2026-07-24" }
				""", as(ayse));
	}

	private org.springframework.test.web.servlet.ResultActions decide(long id, String action, String note)
			throws Exception {
		return mockMvc.perform(post("/api/leave-requests/{id}/{action}", id, action).with(as(manager))
			.contentType(MediaType.APPLICATION_JSON)
			.content(note == null ? "{}" : """
					{ "note": "%s" }
					""".formatted(note)));
	}

}
