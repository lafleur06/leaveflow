package com.batuhanturnay.leaveflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

// Skipped (not failed) on machines without Docker; always runs in CI.
@Testcontainers(disabledWithoutDocker = true)
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class LeaveflowApplicationTests {

	@Test
	void contextLoads() {
	}

}
