package com.batuhanturnay.leaveflow.leave;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.batuhanturnay.leaveflow.leave.dto.LeaveBalanceResponse;

@Tag(name = "Leave requests")
@RestController
public class LeaveBalanceController {

	private final LeaveBalanceService balanceService;

	public LeaveBalanceController(LeaveBalanceService balanceService) {
		this.balanceService = balanceService;
	}

	@Operation(summary = "Annual leave balance of an employee: earned, used, pending and available days")
	@GetMapping("/api/employees/{id}/leave-balance")
	public LeaveBalanceResponse balance(@PathVariable Long id) {
		return balanceService.getBalance(id);
	}

}
