package com.batuhanturnay.leaveflow.calendar;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.batuhanturnay.leaveflow.IntegrationTest;

class HolidayApiTests extends IntegrationTest {

	@Test
	void listsTheSeededHolidaysOfTheCurrentYearByDefault() throws Exception {
		mockMvc.perform(get("/api/holidays"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(14)))
			.andExpect(jsonPath("$[0].date").value("2026-01-01"))
			.andExpect(jsonPath("$[?(@.date == '2026-10-29')].name", hasItem("Republic Day")));
	}

	@Test
	void listsAnotherYear() throws Exception {
		mockMvc.perform(get("/api/holidays").param("year", "2027"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(7)));
	}

	@Test
	void newHolidaysAffectNewLeaveRequests() throws Exception {
		postAndReturnId("/api/holidays", """
				{ "date": "2026-07-14", "name": "Company anniversary" }
				""");
		long department = createDepartment("Engineering");
		long employee = createEmployee("Ayse", "ayse@example.com", department, null);

		// Mon 13 - Fri 17 July: 15 July (national) and 14 July (company) are both excluded
		mockMvc.perform(post("/api/leave-requests").contentType(MediaType.APPLICATION_JSON).content("""
				{ "employeeId": %d, "type": "ANNUAL", "startDate": "2026-07-13", "endDate": "2026-07-17" }
				""".formatted(employee)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.workingDays").value(3));
	}

	@Test
	void rejectsASecondHolidayOnTheSameDate() throws Exception {
		mockMvc.perform(post("/api/holidays").contentType(MediaType.APPLICATION_JSON).content("""
				{ "date": "2026-10-29", "name": "Duplicate" }
				"""))
			.andExpect(status().isConflict());
	}

	@Test
	void deletesAHoliday() throws Exception {
		long id = postAndReturnId("/api/holidays", """
				{ "date": "2026-12-31", "name": "New Year's Eve" }
				""");

		mockMvc.perform(delete("/api/holidays/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/holidays").param("year", "2026")).andExpect(jsonPath("$", hasSize(14)));
	}

}
