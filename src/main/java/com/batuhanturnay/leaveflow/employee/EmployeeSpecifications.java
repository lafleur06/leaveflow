package com.batuhanturnay.leaveflow.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

/** Builds the optional filters of {@code GET /api/employees} into one query. */
final class EmployeeSpecifications {

	private static final char LIKE_ESCAPE = '\\';

	private EmployeeSpecifications() {
	}

	static Specification<Employee> filter(Long departmentId, Boolean active, String query) {
		List<Specification<Employee>> specs = new ArrayList<>();
		if (departmentId != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("department").get("id"), departmentId));
		}
		if (active != null) {
			specs.add((root, cq, cb) -> cb.equal(root.get("active"), active));
		}
		if (query != null && !query.isBlank()) {
			String pattern = "%" + escapeLike(query.strip().toLowerCase(Locale.ROOT)) + "%";
			specs.add((root, cq, cb) -> cb.or(
					cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))),
							pattern, LIKE_ESCAPE),
					cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE)));
		}
		return Specification.allOf(specs);
	}

	/** User input must not be able to inject LIKE wildcards such as % or _. */
	private static String escapeLike(String value) {
		String escape = String.valueOf(LIKE_ESCAPE);
		return value.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_");
	}

}
