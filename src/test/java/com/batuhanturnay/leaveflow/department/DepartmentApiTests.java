package com.batuhanturnay.leaveflow.department;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.batuhanturnay.leaveflow.IntegrationTest;

class DepartmentApiTests extends IntegrationTest {

	@Test
	void createsDepartmentAndReturnsLocation() throws Exception {
		mockMvc
			.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content("""
					{ "name": "  Finance ", "description": "Money matters" }
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern(".*/api/departments/\\d+")))
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("Finance"))
			.andExpect(jsonPath("$.description").value("Money matters"))
			.andExpect(jsonPath("$.createdAt").isNotEmpty());
	}

	@Test
	void rejectsDuplicateNameIgnoringCase() throws Exception {
		createDepartment("Finance");

		mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "FINANCE" }
				"""))
			.andExpect(status().isConflict())
			.andExpect(header().string("Content-Type", "application/problem+json"))
			.andExpect(jsonPath("$.title").value("Conflict"));
	}

	@Test
	void rejectsBlankNameWithFieldError() throws Exception {
		mockMvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "   " }
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors.name").isNotEmpty());
	}

	@Test
	void returnsProblemDetailForUnknownId() throws Exception {
		mockMvc.perform(get("/api/departments/999999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("Department with id 999999 was not found"));
	}

	@Test
	void listsDepartmentsPagedAndSortedByName() throws Exception {
		createDepartment("Sales");
		createDepartment("Engineering");
		createDepartment("Marketing");

		mockMvc.perform(get("/api/departments").param("size", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].name").value("Engineering"))
			.andExpect(jsonPath("$.content[1].name").value("Marketing"))
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.page.totalPages").value(2));
	}

	@Test
	void updatesDepartment() throws Exception {
		long id = createDepartment("Finanse");

		mockMvc.perform(put("/api/departments/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Finance", "description": "Typo fixed" }
				"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Finance"))
			.andExpect(jsonPath("$.description").value("Typo fixed"));
	}

	@Test
	void deletesEmptyDepartment() throws Exception {
		long id = createDepartment("Temporary");

		mockMvc.perform(delete("/api/departments/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/departments/{id}", id)).andExpect(status().isNotFound());
	}

	@Test
	void refusesToDeleteDepartmentWithEmployees() throws Exception {
		long id = createDepartment("Engineering");
		createEmployee("Ayse", "ayse@example.com", id, null);

		mockMvc.perform(delete("/api/departments/{id}", id))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(containsString("still has employees")));
	}

	@Test
	void findsDepartmentById() throws Exception {
		long id = createDepartment("Finance");

		mockMvc.perform(get("/api/departments/{id}", id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id))
			.andExpect(jsonPath("$.name").value("Finance"));
	}

	@Test
	void renamingToAnExistingNameIsRejectedButKeepingTheOwnNameIsNot() throws Exception {
		createDepartment("Finance");
		long id = createDepartment("Sales");

		mockMvc.perform(put("/api/departments/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "FINANCE" }
				""")).andExpect(status().isConflict());
		mockMvc.perform(put("/api/departments/{id}", id).contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Sales", "description": "Same name, new description" }
				""")).andExpect(status().isOk());
	}

	@Test
	void onlyHrManagesDepartments() throws Exception {
		mockMvc.perform(post("/api/departments").with(as(1)).contentType(MediaType.APPLICATION_JSON).content("""
				{ "name": "Shadow IT" }
				""")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/departments").with(as(1))).andExpect(status().isOk());
	}
}
