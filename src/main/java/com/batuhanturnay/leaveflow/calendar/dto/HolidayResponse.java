package com.batuhanturnay.leaveflow.calendar.dto;

import java.time.LocalDate;

import com.batuhanturnay.leaveflow.calendar.PublicHoliday;

public record HolidayResponse(Long id, LocalDate date, String name) {

	public static HolidayResponse from(PublicHoliday holiday) {
		return new HolidayResponse(holiday.getId(), holiday.getDate(), holiday.getName());
	}

}
