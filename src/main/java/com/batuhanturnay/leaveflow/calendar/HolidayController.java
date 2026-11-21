package com.batuhanturnay.leaveflow.calendar;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.batuhanturnay.leaveflow.calendar.dto.HolidayRequest;
import com.batuhanturnay.leaveflow.calendar.dto.HolidayResponse;

@Tag(name = "Public holidays", description = "Days that are not counted as leave")
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

	private final HolidayService holidayService;

	private final Clock clock;

	public HolidayController(HolidayService holidayService, Clock clock) {
		this.holidayService = holidayService;
		this.clock = clock;
	}

	@Operation(summary = "List the public holidays of a year (default: the current year)")
	@GetMapping
	public List<HolidayResponse> list(@RequestParam(required = false) Integer year) {
		return holidayService.findByYear(year != null ? year : LocalDate.now(clock).getYear());
	}

	@Operation(summary = "Add a public holiday (HR)")
	@PreAuthorize("hasRole('HR')")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public HolidayResponse create(@Valid @RequestBody HolidayRequest request) {
		return holidayService.create(request);
	}

	@Operation(summary = "Remove a public holiday (HR)")
	@PreAuthorize("hasRole('HR')")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		holidayService.delete(id);
	}

}
