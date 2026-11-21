package com.batuhanturnay.leaveflow;

import org.springframework.boot.SpringApplication;

public class TestLeaveflowApplication {

	public static void main(String[] args) {
		SpringApplication.from(LeaveflowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
