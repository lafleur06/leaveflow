package com.batuhanturnay.leaveflow.leave;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeService;
import com.batuhanturnay.leaveflow.leave.dto.LeaveBalanceResponse;

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

	private final Clock clock;

	public LeaveBalanceService(LeaveRequestRepository leaveRequestRepository, EmployeeService employeeService,
			Clock clock) {
		this.leaveRequestRepository = leaveRequestRepository;
		this.employeeService = employeeService;
		this.clock = clock;
	}

	public LeaveBalanceResponse getBalance(Long employeeId) {
		return calculate(employeeService.getEntity(employeeId));
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
