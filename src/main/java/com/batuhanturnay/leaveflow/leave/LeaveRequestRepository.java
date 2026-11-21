package com.batuhanturnay.leaveflow.leave;

import java.time.LocalDate;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

	@Override
	@EntityGraph(attributePaths = { "employee", "decidedBy" })
	Page<LeaveRequest> findAll(Specification<LeaveRequest> spec, Pageable pageable);

	/** Two date ranges overlap when each one starts before the other ends. */
	@Query("""
			select count(r) > 0 from LeaveRequest r
			where r.employee.id = :employeeId
			  and r.status in :statuses
			  and r.startDate <= :endDate
			  and r.endDate >= :startDate
			""")
	boolean existsOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate,
			Collection<LeaveStatus> statuses);

	@Query("""
			select coalesce(sum(r.workingDays), 0) from LeaveRequest r
			where r.employee.id = :employeeId and r.type = :type and r.status = :status
			""")
	int sumWorkingDays(Long employeeId, LeaveType type, LeaveStatus status);

}
