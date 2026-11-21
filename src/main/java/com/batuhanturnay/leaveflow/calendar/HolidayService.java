package com.batuhanturnay.leaveflow.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batuhanturnay.leaveflow.calendar.dto.HolidayRequest;
import com.batuhanturnay.leaveflow.calendar.dto.HolidayResponse;
import com.batuhanturnay.leaveflow.common.error.ConflictException;
import com.batuhanturnay.leaveflow.common.error.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class HolidayService {

	private final PublicHolidayRepository holidayRepository;

	public HolidayService(PublicHolidayRepository holidayRepository) {
		this.holidayRepository = holidayRepository;
	}

	public List<HolidayResponse> findByYear(int year) {
		return holidayRepository.findByDateBetweenOrderByDate(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
			.stream()
			.map(HolidayResponse::from)
			.toList();
	}

	/** Working days in {@code [start, end]}, excluding weekends and the public holidays in the database. */
	public int workingDaysBetween(LocalDate start, LocalDate end) {
		Set<LocalDate> holidays = holidayRepository.findByDateBetweenOrderByDate(start, end)
			.stream()
			.map(PublicHoliday::getDate)
			.collect(Collectors.toSet());
		return WorkingDays.count(start, end, holidays);
	}

	/**
	 * Adding or removing a holiday does not change leave requests that already exist:
	 * their working-day count is fixed when they are created.
	 */
	@Transactional
	public HolidayResponse create(HolidayRequest request) {
		if (holidayRepository.existsByDate(request.date())) {
			throw new ConflictException("A public holiday on %s already exists".formatted(request.date()));
		}
		return HolidayResponse.from(holidayRepository.save(new PublicHoliday(request.date(), request.name().strip())));
	}

	@Transactional
	public void delete(Long id) {
		PublicHoliday holiday = holidayRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Public holiday", id));
		holidayRepository.delete(holiday);
	}

}
