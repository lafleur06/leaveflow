package com.batuhanturnay.leaveflow.leave;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.common.error.ForbiddenException;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeService;
import com.batuhanturnay.leaveflow.leave.dto.LeaveBalanceResponse;
import com.batuhanturnay.leaveflow.security.CurrentUser;

/**
 * The balance is calculated, not stored: earned days come from the hire and birth dates,
 * used and pending days from the leave requests. Nothing can drift out of sync, and no
 * yearly batch job is needed to add new entitlement on each anniversary.
 */
@Service
@Transactional(readOnly = true)
public class LeaveBalanceService {

	private final LeaveRequestRepository leaveRequestRepository;

	private final EmployeeService employeeService;

	private final CurrentUser currentUser;

	private final Clock clock;

	public LeaveBalanceService(LeaveRequestRepository leaveRequestRepository, EmployeeService employeeService,
			CurrentUser currentUser, Clock clock) {
		this.leaveRequestRepository = leaveRequestRepository;
		this.employeeService = employeeService;
		this.currentUser = currentUser;
		this.clock = clock;
	}

	/** Visible to the employee, their direct manager and HR. */
	public LeaveBalanceResponse getBalance(Long employeeId) {
		Employee employee = employeeService.getEntity(employeeId);
		if (!LeaveAccess.canView(currentUser.get(), employee)) {
			throw new ForbiddenException("You may not view the leave balance of employee %d".formatted(employeeId));
		}
		return calculate(employee);
	}

	LeaveBalanceResponse calculate(Employee employee) {
		AnnualLeavePolicy.Accrual accrual = AnnualLeavePolicy.accrualAsOf(employee.getBirthDate(),
				employee.getHireDate(), LocalDate.now(clock));
		int used = leaveRequestRepository.sumWorkingDays(employee.getId(), LeaveType.ANNUAL, LeaveStatus.APPROVED);
		int pending = leaveRequestRepository.sumWorkingDays(employee.getId(), LeaveType.ANNUAL, LeaveStatus.PENDING);
		return new LeaveBalanceResponse(employee.getId(), accrual.completedServiceYears(), accrual.earnedDays(),
				used, pending, accrual.earnedDays() - used - pending, accrual.nextAccrualDate(),
				accrual.nextAccrualDays());
	}

}
