package com.batuhanturnay.leaveflow;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class OpenApiDocsTests extends IntegrationTest {

	@Test
	void publishesOpenApiDocumentForAllEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.info.title").value("LeaveFlow API"))
			.andExpect(jsonPath("$.paths['/api/departments'].get.summary").value("List departments"))
			.andExpect(jsonPath("$.paths['/api/employees/{id}'].delete").exists())
			// Pageable is exposed as separate page/size/sort query parameters, not as one JSON object
			.andExpect(jsonPath("$.paths['/api/employees'].get.parameters[?(@.name == 'size')]").exists());
	}

	@Test
	void servesSwaggerUi() throws Exception {
		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/swagger-ui/index.html"));

		mockMvc.perform(get("/swagger-ui/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("swagger-ui")));
	}

}
