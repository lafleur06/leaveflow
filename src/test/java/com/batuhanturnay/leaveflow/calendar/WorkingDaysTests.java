package com.batuhanturnay.leaveflow.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WorkingDaysTests {

	private static final Set<LocalDate> NO_HOLIDAYS = Set.of();

	@Test
	void countsAFullWorkWeek() {
		// Monday 13 July 2026 to Friday 17 July 2026
		assertThat(WorkingDays.count(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 17), NO_HOLIDAYS)).isEqualTo(5);
	}

	@Test
	void bothEndsAreInclusive() {
		LocalDate monday = LocalDate.of(2026, 6, 15);
		assertThat(WorkingDays.count(monday, monday, NO_HOLIDAYS)).isEqualTo(1);
	}

	@Test
	void skipsWeekends() {
		// Friday 19 June to Monday 22 June 2026
		assertThat(WorkingDays.count(LocalDate.of(2026, 6, 19), LocalDate.of(2026, 6, 22), NO_HOLIDAYS)).isEqualTo(2);
	}

	@Test
	void aWeekendOnlyRangeHasNoWorkingDays() {
		assertThat(WorkingDays.count(LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 21), NO_HOLIDAYS)).isZero();
	}

	@Test
	void skipsHolidaysOnWeekdays() {
		// 15 July 2026 (Wednesday) is Democracy and National Unity Day
		Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 7, 15));
		assertThat(WorkingDays.count(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 17), holidays)).isEqualTo(4);
	}

	@Test
	void doesNotSubtractAHolidayThatFallsOnAWeekendTwice() {
		// 30 August 2026 (Victory Day) is a Sunday
		Set<LocalDate> holidays = Set.of(LocalDate.of(2026, 8, 30));
		assertThat(WorkingDays.count(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30), holidays)).isEqualTo(5);
	}

	@Test
	void rejectsReversedRange() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> WorkingDays.count(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 1), NO_HOLIDAYS));
	}

}
