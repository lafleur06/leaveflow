package com.batuhanturnay.leaveflow.employee;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.batuhanturnay.leaveflow.IntegrationTest;

class EmployeeApiTests extends IntegrationTest {

	private long engineering;

	@BeforeEach
	void setUp() throws Exception {
		engineering = createDepartment("Engineering");
	}

	@Test
	void createsEmployeeWithManager() throws Exception {
		long managerId = createEmployee("Mehmet", "mehmet@example.com", engineering, null);

		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Yilmaz", "Ayse.Yilmaz@Example.com", engineering, managerId,
						"1995-03-15", "2023-09-01")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("ayse.yilmaz@example.com"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.department.name").value("Engineering"))
			.andExpect(jsonPath("$.manager.id").value(managerId))
			.andExpect(jsonPath("$.manager.name").value("Mehmet Test"));
	}

	@Test
	void rejectsDuplicateEmailIgnoringCase() throws Exception {
		createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Other", "Person", "AYSE@example.com", engineering, null, "1990-01-01",
						"2020-01-01")))
			.andExpect(status().isConflict());
	}

	@Test
	void reportsEveryInvalidField() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("""
				{ "firstName": "", "lastName": "Yilmaz", "email": "not-an-email", "birthDate": "2999-01-01" }
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.firstName").isNotEmpty())
			.andExpect(jsonPath("$.errors.email").isNotEmpty())
			.andExpect(jsonPath("$.errors.birthDate").isNotEmpty())
			.andExpect(jsonPath("$.errors.hireDate").isNotEmpty())
			.andExpect(jsonPath("$.errors.departmentId").isNotEmpty());
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void rejectsEmployeeYoungerThanMinimumWorkingAge() throws Exception {
		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Young", "Person", "young@example.com", engineering, null, "2010-06-01",
						"2024-06-01")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("at least 15 years old")));
	}

	@Test
	void returnsNotFoundForUnknownDepartment() throws Exception {
		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Yilmaz", "ayse@example.com", 999999, null, "1995-03-15",
						"2023-09-01")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Department with id 999999 was not found"));
	}

	@Test
	void preventsSelfManagement() throws Exception {
		long id = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc
			.perform(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Test", "ayse@example.com", engineering, id, "1990-05-10",
						"2020-01-06")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value("An employee cannot be their own manager"));
	}

	@Test
	void preventsReportingCycles() throws Exception {
		long head = createEmployee("Head", "head@example.com", engineering, null);
		long lead = createEmployee("Lead", "lead@example.com", engineering, head);
		long dev = createEmployee("Dev", "dev@example.com", engineering, lead);

		// head -> lead -> dev already exists; making dev the head's manager closes the loop
		mockMvc
			.perform(put("/api/employees/{id}", head).contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Head", "Test", "head@example.com", engineering, dev, "1990-05-10",
						"2020-01-06")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.detail").value(containsString("reporting cycle")));
	}

	@Test
	void deactivationIsBlockedWhileEmployeeManagesActiveStaff() throws Exception {
		long manager = createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		long report = createEmployee("Ayse", "ayse@example.com", engineering, manager);

		mockMvc.perform(delete("/api/employees/{id}", manager)).andExpect(status().isConflict());

		mockMvc.perform(delete("/api/employees/{id}", report)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/employees/{id}", manager)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/employees/{id}", manager))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void inactiveEmployeeCannotBecomeManager() throws Exception {
		long former = createEmployee("Former", "former@example.com", engineering, null);
		mockMvc.perform(delete("/api/employees/{id}", former)).andExpect(status().isNoContent());

		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("New", "Hire", "new@example.com", engineering, former, "1998-01-01",
						"2025-01-01")))
			.andExpect(status().isUnprocessableContent());
	}

	@Test
	void filtersByDepartmentActiveFlagAndText() throws Exception {
		long sales = createDepartment("Sales");
		createEmployee("Ayse", "ayse@example.com", engineering, null);
		createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		long left = createEmployee("Zeynep", "zeynep@example.com", sales, null);
		mockMvc.perform(delete("/api/employees/{id}", left)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/employees").param("departmentId", String.valueOf(engineering)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)));

		mockMvc.perform(get("/api/employees").param("active", "false"))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].firstName").value("Zeynep"));

		mockMvc.perform(get("/api/employees").param("q", "MEH"))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].email").value("mehmet@example.com"));
	}

	@Test
	void treatsLikeWildcardsInSearchAsPlainText() throws Exception {
		createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc.perform(get("/api/employees").param("q", "%"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(0)));
	}

	@Test
	void rejectsUnknownSortProperty() throws Exception {
		mockMvc.perform(get("/api/employees").param("sort", "salary,desc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid sort property"));
	}

	@Test
	void rejectsNonNumericFilter() throws Exception {
		mockMvc.perform(get("/api/employees").param("departmentId", "abc")).andExpect(status().isBadRequest());
	}

	@Test
	void updatesEmployee() throws Exception {
		long sales = createDepartment("Sales");
		long manager = createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		long id = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc
			.perform(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson(" Ayse ", "Yilmaz", "AYSE.Yilmaz@Example.com", sales, manager, "1990-05-10",
						"2020-01-06")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.firstName").value("Ayse"))
			.andExpect(jsonPath("$.lastName").value("Yilmaz"))
			.andExpect(jsonPath("$.email").value("ayse.yilmaz@example.com"))
			.andExpect(jsonPath("$.department.name").value("Sales"))
			.andExpect(jsonPath("$.manager.id").value(manager));
	}

	@Test
	void rejectsUpdateToAnotherEmployeesEmail() throws Exception {
		createEmployee("Mehmet", "mehmet@example.com", engineering, null);
		long id = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc
			.perform(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Test", "Mehmet@example.com", engineering, null, "1990-05-10",
						"2020-01-06")))
			.andExpect(status().isConflict());
	}

	@Test
	void keepingTheOwnEmailOnUpdateIsNotADuplicate() throws Exception {
		long id = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc
			.perform(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Test", "ayse@example.com", engineering, null, "1990-05-10",
						"2021-03-01")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hireDate").value("2021-03-01"));
	}

	@Test
	void deactivatingTwiceIsHarmless() throws Exception {
		long id = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc.perform(delete("/api/employees/{id}", id)).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/employees/{id}", id)).andExpect(status().isNoContent());
	}

	@Test
	void returnsNotFoundForUnknownEmployeeOrManager() throws Exception {
		mockMvc.perform(get("/api/employees/{id}", 424242)).andExpect(status().isNotFound());
		mockMvc
			.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(employeeJson("Ayse", "Test", "ayse@example.com", engineering, 424242L, "1990-05-10",
						"2020-01-06")))
			.andExpect(status().isNotFound());
	}

	@Test
	void birthDateIsHiddenFromColleagues() throws Exception {
		long ayse = createEmployee("Ayse", "ayse@example.com", engineering, null);
		long colleague = createEmployee("Can", "can@example.com", engineering, null);

		mockMvc.perform(get("/api/employees/{id}", ayse).with(as(ayse)))
			.andExpect(jsonPath("$.birthDate").value("1990-05-10"));
		mockMvc.perform(get("/api/employees/{id}", ayse).with(as(colleague)))
			.andExpect(jsonPath("$.birthDate").doesNotExist());
	}

	@Test
	void employeesCannotChangeStaffRecords() throws Exception {
		long ayse = createEmployee("Ayse", "ayse@example.com", engineering, null);

		mockMvc.perform(delete("/api/employees/{id}", ayse).with(as(ayse))).andExpect(status().isForbidden());
		mockMvc
			.perform(put("/api/employees/{id}/role", ayse).with(as(ayse))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "role": "HR" }
						"""))
			.andExpect(status().isForbidden());
	}
}
