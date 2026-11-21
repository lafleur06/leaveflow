package com.batuhanturnay.leaveflow.leave;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.calendar.HolidayService;
import com.batuhanturnay.leaveflow.common.error.BusinessRuleException;
import com.batuhanturnay.leaveflow.common.error.ConflictException;
import com.batuhanturnay.leaveflow.common.error.ForbiddenException;
import com.batuhanturnay.leaveflow.common.error.ResourceNotFoundException;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.employee.EmployeeService;
import com.batuhanturnay.leaveflow.leave.dto.LeaveBalanceResponse;
import com.batuhanturnay.leaveflow.leave.dto.LeaveDecisionRequest;
import com.batuhanturnay.leaveflow.leave.dto.LeaveRequestCreateRequest;
import com.batuhanturnay.leaveflow.leave.dto.LeaveRequestResponse;

@Service
@Transactional(readOnly = true)
public class LeaveRequestService {

	private final LeaveRequestRepository leaveRequestRepository;

	private final EmployeeRepository employeeRepository;

	private final EmployeeService employeeService;

	private final HolidayService holidayService;

	private final LeaveBalanceService balanceService;

	private final Clock clock;

	public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository,
			EmployeeService employeeService, HolidayService holidayService, LeaveBalanceService balanceService,
			Clock clock) {
		this.leaveRequestRepository = leaveRequestRepository;
		this.employeeRepository = employeeRepository;
		this.employeeService = employeeService;
		this.holidayService = holidayService;
		this.balanceService = balanceService;
		this.clock = clock;
	}

	public Page<LeaveRequestResponse> search(Long employeeId, Long managerId, LeaveStatus status, LeaveType type,
			Pageable pageable) {
		return leaveRequestRepository
			.findAll(LeaveRequestSpecifications.filter(employeeId, managerId, status, type), pageable)
			.map(LeaveRequestResponse::from);
	}

	public LeaveRequestResponse findById(Long id) {
		return LeaveRequestResponse.from(getEntity(id));
	}

	@Transactional
	public LeaveRequestResponse create(LeaveRequestCreateRequest request) {
		// Lock the employee first: parallel requests for the same person are processed one after another,
		// so the overlap and balance checks below always see each other's results.
		Employee employee = employeeRepository.findByIdForUpdate(request.employeeId())
			.orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));
		LocalDate start = request.startDate();
		LocalDate end = request.endDate();
		LeaveType type = request.type();
		LocalDate today = LocalDate.now(clock);

		if (!employee.isActive()) {
			throw new BusinessRuleException("Employee %d is not active".formatted(employee.getId()));
		}
		if (end.isBefore(start)) {
			throw new BusinessRuleException("endDate must not be before startDate");
		}
		if (start.isBefore(employee.getHireDate())) {
			throw new BusinessRuleException("Leave cannot start before the hire date (%s)".formatted(employee.getHireDate()));
		}
		if (!type.allowsPastDates() && start.isBefore(today)) {
			throw new BusinessRuleException("%s leave cannot start in the past".formatted(type));
		}
		int workingDays = holidayService.workingDaysBetween(start, end);
		if (workingDays == 0) {
			throw new BusinessRuleException(
					"The period %s to %s contains no working days (only weekends or public holidays)".formatted(start,
							end));
		}
		if (leaveRequestRepository.existsOverlapping(employee.getId(), start, end, LeaveStatus.ACTIVE)) {
			throw new ConflictException("Employee %d already has a pending or approved leave request in this period"
				.formatted(employee.getId()));
		}
		if (type.deductsFromBalance()) {
			LeaveBalanceResponse balance = balanceService.calculate(employee);
			if (workingDays > balance.availableDays()) {
				throw new BusinessRuleException(
						"Insufficient annual leave: %d working days requested, %d available".formatted(workingDays,
								balance.availableDays()));
			}
		}

		LeaveRequest saved = leaveRequestRepository
			.save(new LeaveRequest(employee, type, start, end, workingDays, request.reason()));
		return LeaveRequestResponse.from(saved);
	}

	@Transactional
	public LeaveRequestResponse approve(Long id, LeaveDecisionRequest decision) {
		LeaveRequest request = getEntity(id);
		Employee approver = checkApprover(request, decision.approverId());
		request.approve(approver, decision.note(), now());
		return LeaveRequestResponse.from(leaveRequestRepository.saveAndFlush(request));
	}

	@Transactional
	public LeaveRequestResponse reject(Long id, LeaveDecisionRequest decision) {
		if (decision.note() == null || decision.note().isBlank()) {
			throw new BusinessRuleException("A note explaining the rejection is required");
		}
		LeaveRequest request = getEntity(id);
		Employee approver = checkApprover(request, decision.approverId());
		request.reject(approver, decision.note().strip(), now());
		return LeaveRequestResponse.from(leaveRequestRepository.saveAndFlush(request));
	}

	@Transactional
	public LeaveRequestResponse cancel(Long id) {
		LeaveRequest request = getEntity(id);
		request.cancel(LocalDate.now(clock));
		return LeaveRequestResponse.from(leaveRequestRepository.saveAndFlush(request));
	}

	private LeaveRequest getEntity(Long id) {
		return leaveRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request", id));
	}

	/**
	 * Only the employee's direct manager may decide. Employees without a manager cannot be
	 * approved yet: the HR role that covers them arrives with authentication in Phase 3.
	 */
	private Employee checkApprover(LeaveRequest request, Long approverId) {
		Employee employee = request.getEmployee();
		if (employee.getId().equals(approverId)) {
			throw new ForbiddenException("Employees cannot decide on their own leave requests");
		}
		Employee manager = employee.getManager();
		if (manager == null) {
			throw new BusinessRuleException(
					"Employee %d has no manager to decide on this request".formatted(employee.getId()));
		}
		if (!manager.getId().equals(approverId)) {
			throw new ForbiddenException(
					"Only the employee's direct manager (employee %d) can decide on this request".formatted(manager.getId()));
		}
		return employeeService.getEntity(approverId);
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
