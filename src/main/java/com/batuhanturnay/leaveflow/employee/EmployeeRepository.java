package com.batuhanturnay.leaveflow.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

	/**
	 * Loads department and manager in the same query. Without the entity graph,
	 * a page of 20 employees would trigger up to 40 extra queries (the N+1 problem).
	 */
	@Override
	@EntityGraph(attributePaths = { "department", "manager" })
	Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	boolean existsByDepartmentId(Long departmentId);

	boolean existsByManagerIdAndActiveTrue(Long managerId);

}
