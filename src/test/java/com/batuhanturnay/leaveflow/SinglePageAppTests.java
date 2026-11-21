package com.batuhanturnay.leaveflow;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * The bundled React client (a stand-in index.html in the test resources). Its pages are public;
 * the data behind them is not.
 */
class SinglePageAppTests extends IntegrationTest {

	@Test
	void clientPagesLoadWithoutAToken() throws Exception {
		noAuthMvc.perform(get("/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("LeaveFlow test client")));
		noAuthMvc.perform(get("/approvals")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		noAuthMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		noAuthMvc.perform(get("/people/42")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
		noAuthMvc.perform(get("/reports")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
	}

	@Test
	void theApiStillNeedsAToken() throws Exception {
		noAuthMvc.perform(get("/api/leave-requests")).andExpect(status().isUnauthorized());
	}

	@Test
	void demoAccountsDoNotExistOutsideTheDemo() throws Exception {
		noAuthMvc.perform(get("/api/demo/accounts")).andExpect(status().isNotFound());
	}

}
