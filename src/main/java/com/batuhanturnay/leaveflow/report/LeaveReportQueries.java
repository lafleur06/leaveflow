package com.batuhanturnay.leaveflow.report;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.leave.LeaveRequest;
import com.batuhanturnay.leaveflow.leave.LeaveStatus;
import com.batuhanturnay.leaveflow.leave.LeaveType;
import com.batuhanturnay.leaveflow.report.dto.LeaveUsageRow;

/**
 * The read-only queries behind the reports. Each report needs a fixed number of queries,
 * however many employees or requests there are (no N+1).
 */
@Repository
class LeaveReportQueries {

	private final EntityManager entityManager;

	LeaveReportQueries(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	List<Employee> activeEmployees(Long departmentId) {
		return entityManager.createQuery("""
				select e from Employee e
				join fetch e.department d
				left join fetch e.manager
				where e.active = true and (:departmentId is null or d.id = :departmentId)
				order by d.name, e.lastName, e.firstName, e.id
				""", Employee.class).setParameter("departmentId", departmentId).getResultList();
	}

	/** Annual leave days per employee, split into approved and pending. */
	Map<Long, AnnualDays> annualDaysByEmployee(Long departmentId) {
		List<Object[]> rows = entityManager.createQuery("""
				select r.employee.id, r.status, sum(r.workingDays) from LeaveRequest r
				where r.type = :type and r.status in :statuses
				  and (:departmentId is null or r.employee.department.id = :departmentId)
				group by r.employee.id, r.status
				""", Object[].class)
			.setParameter("type", LeaveType.ANNUAL)
			.setParameter("statuses", LeaveStatus.ACTIVE)
			.setParameter("departmentId", departmentId)
			.getResultList();
		Map<Long, AnnualDays> days = new HashMap<>();
		for (Object[] row : rows) {
			long employeeId = (Long) row[0];
			int sum = ((Number) row[2]).intValue();
			AnnualDays current = days.getOrDefault(employeeId, AnnualDays.NONE);
			days.put(employeeId,
					row[1] == LeaveStatus.APPROVED ? current.withUsed(sum) : current.withPending(sum));
		}
		return days;
	}

	/** Approved leave that starts within the period, per department and type. */
	List<LeaveUsageRow> usage(LocalDate from, LocalDate to) {
		return entityManager.createQuery("""
				select new com.batuhanturnay.leaveflow.report.dto.LeaveUsageRow(
				    d.name, r.type, count(r), count(distinct e.id), sum(r.workingDays))
				from LeaveRequest r join r.employee e join e.department d
				where r.status = :approved and r.startDate between :from and :to
				group by d.name, r.type
				order by d.name, r.type
				""", LeaveUsageRow.class)
			.setParameter("approved", LeaveStatus.APPROVED)
			.setParameter("from", from)
			.setParameter("to", to)
			.getResultList();
	}

	/** Approved leave overlapping the period. */
	List<LeaveRequest> approvedLeaveOverlapping(LocalDate from, LocalDate to) {
		return entityManager.createQuery("""
				select r from LeaveRequest r
				join fetch r.employee e
				join fetch e.department d
				where r.status = :approved and r.startDate <= :to and r.endDate >= :from
				order by r.startDate, d.name, e.lastName, e.firstName, r.id
				""", LeaveRequest.class)
			.setParameter("approved", LeaveStatus.APPROVED)
			.setParameter("from", from)
			.setParameter("to", to)
			.getResultList();
	}

	record AnnualDays(int used, int pending) {

		static final AnnualDays NONE = new AnnualDays(0, 0);

		AnnualDays withUsed(int days) {
			return new AnnualDays(days, pending);
		}

		AnnualDays withPending(int days) {
			return new AnnualDays(used, days);
		}

	}

}
