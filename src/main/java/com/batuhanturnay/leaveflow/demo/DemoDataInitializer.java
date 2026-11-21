package com.batuhanturnay.leaveflow.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.calendar.HolidayService;
import com.batuhanturnay.leaveflow.department.Department;
import com.batuhanturnay.leaveflow.department.DepartmentRepository;
import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.employee.EmployeeRepository;
import com.batuhanturnay.leaveflow.employee.Role;
import com.batuhanturnay.leaveflow.leave.LeaveRequest;
import com.batuhanturnay.leaveflow.leave.LeaveRequestRepository;
import com.batuhanturnay.leaveflow.leave.LeaveType;

/**
 * Fills an empty database with a small company, so the demo has something to show on first login.
 * <p>
 * All dates are relative to today, so the demo always has leave in the past, waiting for
 * approval and coming up. Requests go through the entity's own state transitions, so their
 * history (audit trail) is recorded as for real requests. Runs before the bootstrap HR account,
 * which then sees that an HR account exists and does nothing.
 */
@Component
@Profile("demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
class DemoDataInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

	static final String DOMAIN = "@leaveflow.demo";

	static final String HR_EMAIL = "zeynep.arslan" + DOMAIN;

	static final String MANAGER_EMAIL = "mehmet.kaya" + DOMAIN;

	static final String EMPLOYEE_EMAIL = "ayse.yilmaz" + DOMAIN;

	/** The accounts the login page offers; the other employees exist to make the data realistic. */
	static final List<String> LOGIN_ACCOUNTS = List.of(EMPLOYEE_EMAIL, MANAGER_EMAIL, HR_EMAIL);

	private final DepartmentRepository departments;

	private final EmployeeRepository employees;

	private final LeaveRequestRepository leaveRequests;

	private final HolidayService holidays;

	private final PasswordEncoder passwordEncoder;

	private final DemoProperties properties;

	private final Clock clock;

	DemoDataInitializer(DepartmentRepository departments, EmployeeRepository employees,
			LeaveRequestRepository leaveRequests, HolidayService holidays, PasswordEncoder passwordEncoder,
			DemoProperties properties, Clock clock) {
		this.departments = departments;
		this.employees = employees;
		this.leaveRequests = leaveRequests;
		this.holidays = holidays;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (departments.count() > 0 || employees.count() > 0) {
			log.info("Demo profile: the database already has data, nothing seeded");
			return;
		}
		LocalDate today = LocalDate.now(clock);
		String passwordHash = passwordEncoder.encode(properties.password());

		Department engineering = departments.save(new Department("Engineering", "Product development"));
		Department sales = departments.save(new Department("Sales", "Customers and partners"));
		Department people = departments.save(new Department("Human Resources", "People and leave management"));

		Employee zeynep = employee("Zeynep", "Arslan", HR_EMAIL, "HR Manager", today.minusYears(9).minusMonths(4),
				LocalDate.of(1985, 3, 14), people, null);
		zeynep.changeRole(Role.HR);
		zeynep.changePasswordHash(passwordHash);
		Employee mehmet = employee("Mehmet", "Kaya", MANAGER_EMAIL, "Engineering Manager",
				today.minusYears(7).minusMonths(2), LocalDate.of(1984, 11, 2), engineering, null);
		mehmet.changePasswordHash(passwordHash);
		Employee ayse = employee("Ayşe", "Yılmaz", EMPLOYEE_EMAIL, "Software Engineer",
				today.minusYears(3).minusMonths(5), LocalDate.of(1996, 7, 11), engineering, mehmet);
		ayse.changePasswordHash(passwordHash);
		Employee can = employee("Can", "Demir", "can.demir" + DOMAIN, "Backend Developer", today.minusMonths(8),
				LocalDate.of(2000, 2, 20), engineering, mehmet);
		// 16 years of service: the 26-day tier of Art. 53
		Employee elif = employee("Elif", "Şahin", "elif.sahin" + DOMAIN, "Sales Director",
				today.minusYears(16).minusMonths(1), LocalDate.of(1979, 9, 5), sales, null);
		// over 50: at least 20 days a year even with short service
		Employee burak = employee("Burak", "Çelik", "burak.celik" + DOMAIN, "Account Executive",
				today.minusYears(4).minusMonths(3), today.minusYears(52), sales, elif);
		employees.saveAll(List.of(zeynep, mehmet, ayse, can, elif, burak));

		LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

		// Ayşe: a past week off, a refused request, and one waiting for Mehmet
		approved(ayse, LeaveType.ANNUAL, thisMonday.minusWeeks(7), 5, mehmet, "Enjoy the break");
		rejected(ayse, LeaveType.ANNUAL, thisMonday.plusWeeks(2), 5, mehmet,
				"Release week; could you take the week after?");
		pending(ayse, LeaveType.ANNUAL, thisMonday.plusWeeks(3), 3);
		// Can has no annual leave yet (first year), so he asks for unpaid leave
		pending(can, LeaveType.UNPAID, thisMonday.plusWeeks(5).plusDays(3), 2);
		// Mehmet has no manager, so HR decided his request
		approved(mehmet, LeaveType.ANNUAL, thisMonday.plusWeeks(1), 5, zeynep, null);
		approved(burak, LeaveType.SICK, thisMonday.minusWeeks(1).plusDays(1), 2, elif, "Get well soon");
		// Elif has no manager either: her request waits for HR
		pending(elif, LeaveType.ANNUAL, thisMonday.plusWeeks(6), 4);

		log.info("Demo profile: seeded 3 departments, 6 employees and their leave. Log in as {}", LOGIN_ACCOUNTS);
	}

	private Employee employee(String firstName, String lastName, String email, String jobTitle, LocalDate hireDate,
			LocalDate birthDate, Department department, Employee manager) {
		return new Employee(firstName, lastName, email, jobTitle, hireDate, birthDate, department, manager);
	}

	private LeaveRequest submit(Employee employee, LeaveType type, LocalDate start, int days) {
		LocalDate end = start.plusDays(days - 1);
		int workingDays = holidays.workingDaysBetween(start, end);
		if (workingDays == 0) {
			return null; // the period fell on public holidays this year; leave it out
		}
		// submitted a few days before the leave starts, but never in the future
		Instant submittedAt = notAfterNow(start.minusDays(12));
		return leaveRequests
			.save(new LeaveRequest(employee, type, start, end, workingDays, "Demo request", submittedAt));
	}

	private void pending(Employee employee, LeaveType type, LocalDate start, int days) {
		submit(employee, type, start, days);
	}

	private void approved(Employee employee, LeaveType type, LocalDate start, int days, Employee decider,
			String note) {
		LeaveRequest request = submit(employee, type, start, days);
		if (request != null) {
			request.approve(decider, note, decidedAt(request));
			leaveRequests.save(request);
		}
	}

	private void rejected(Employee employee, LeaveType type, LocalDate start, int days, Employee decider,
			String note) {
		LeaveRequest request = submit(employee, type, start, days);
		if (request != null) {
			request.reject(decider, note, decidedAt(request));
			leaveRequests.save(request);
		}
	}

	private Instant decidedAt(LeaveRequest request) {
		return notAfterNow(request.getStartDate().minusDays(10));
	}

	/** 10:00 on the given day in the business time zone, but no later than now. */
	private Instant notAfterNow(LocalDate day) {
		Instant now = clock.instant();
		Instant at = day.atTime(10, 0).atZone(clock.getZone()).toInstant().truncatedTo(ChronoUnit.MICROS);
		return at.isAfter(now) ? now.truncatedTo(ChronoUnit.MICROS) : at;
	}

}
