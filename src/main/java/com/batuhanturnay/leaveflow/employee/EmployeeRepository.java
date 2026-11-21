package com.batuhanturnay.leaveflow.employee;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	/**
	 * Loads department and manager in the same query. Without the entity graph,
	 * a page of 20 employees would trigger up to 40 extra queries (the N+1 problem).
	 */
	@Override
	@EntityGraph(attributePaths = { "department", "manager" })
	Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

	/**
	 * Locks the employee row ({@code SELECT ... FOR UPDATE}) until the transaction ends.
	 * Used when booking leave, so two parallel requests cannot both spend the same balance.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Employee e where e.id = :id")
	Optional<Employee> findByIdForUpdate(Long id);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	boolean existsByDepartmentId(Long departmentId);

	boolean existsByManagerIdAndActiveTrue(Long managerId);

}
