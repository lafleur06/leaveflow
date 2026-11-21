package com.batuhanturnay.leaveflow.leave;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;

import com.batuhanturnay.leaveflow.employee.Employee;

/** Builds the optional filters of {@code GET /api/leave-requests} into one query. */
final class LeaveRequestSpecifications {

	private LeaveRequestSpecifications() {
	}

	/**
	 * @param managerId only requests of this manager's direct reports, e.g. a manager's approval queue
	 */
	static Specification<LeaveRequest> filter(Long employeeId, Long managerId, LeaveStatus status, LeaveType type) {
		List<Specification<LeaveRequest>> specs = new ArrayList<>();
		if (employeeId != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
		}
		if (managerId != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("employee").get("manager").get("id"), managerId));
		}
		if (status != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("status"), status));
		}
		if (type != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("type"), type));
		}
		return Specification.allOf(specs);
	}

	/**
	 * What a non-HR user may see: their own requests and those of their direct reports.
	 * The manager join is a LEFT join, otherwise employees without a manager would lose
	 * sight of their own requests.
	 */
	static Specification<LeaveRequest> visibleTo(long userId) {
		return (root, cq, cb) -> {
			Join<LeaveRequest, Employee> employee = root.join("employee");
			Join<Employee, Employee> manager = employee.join("manager", JoinType.LEFT);
			return cb.or(cb.equal(employee.get("id"), userId), cb.equal(manager.get("id"), userId));
		};
	}

}
