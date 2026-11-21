package com.batuhanturnay.leaveflow.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Pure working-day arithmetic, with no database access, so it can be unit-tested directly.
 * Saturday and Sunday are treated as non-working days (a five-day working week).
 */
public final class WorkingDays {

	private WorkingDays() {
	}

	/**
	 * Counts the working days in {@code [start, end]}, both ends inclusive.
	 * @param holidays public holidays to exclude; weekend holidays are not double-counted
	 */
	public static int count(LocalDate start, LocalDate end, Set<LocalDate> holidays) {
		if (end.isBefore(start)) {
			throw new IllegalArgumentException("end (%s) is before start (%s)".formatted(end, start));
		}
		return (int) start.datesUntil(end.plusDays(1))
			.filter(day -> !isWeekend(day) && !holidays.contains(day))
			.count();
	}

	static boolean isWeekend(LocalDate day) {
		DayOfWeek dayOfWeek = day.getDayOfWeek();
		return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
	}

}
