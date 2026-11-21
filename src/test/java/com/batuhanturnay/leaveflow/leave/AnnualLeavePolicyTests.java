package com.batuhanturnay.leaveflow.leave;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnnualLeavePolicyTests {

	@ParameterizedTest(name = "service year {0}, age {1} -> {2} days")
	@CsvSource({
			// no leave is earned before the first anniversary
			"0, 30, 0",
			// 1 to 5 years (5 included): 14 days
			"1, 30, 14", "5, 30, 14",
			// more than 5, less than 15 years: 20 days
			"6, 30, 20", "14, 40, 20",
			// 15 years and more: 26 days
			"15, 40, 26", "30, 49, 26",
			// aged 18 or younger, or 50 and older: at least 20 days
			"1, 18, 20", "1, 19, 14", "3, 50, 20", "3, 49, 14",
			// the age floor never lowers a higher entitlement
			"15, 55, 26" })
	void entitlementFollowsLabourLawArticle53(int serviceYear, int age, int expectedDays) {
		assertThat(AnnualLeavePolicy.entitlementFor(serviceYear, age)).isEqualTo(expectedDays);
	}

	@Test
	void earnsNothingInTheFirstYear() {
		AnnualLeavePolicy.Accrual accrual = AnnualLeavePolicy.accrualAsOf(LocalDate.of(1998, 1, 1),
				LocalDate.of(2026, 1, 5), LocalDate.of(2026, 6, 15));

		assertThat(accrual.completedServiceYears()).isZero();
		assertThat(accrual.earnedDays()).isZero();
		assertThat(accrual.nextAccrualDate()).isEqualTo(LocalDate.of(2027, 1, 5));
		assertThat(accrual.nextAccrualDays()).isEqualTo(14);
	}

	@Test
	void earnsOnTheAnniversaryItself() {
		AnnualLeavePolicy.Accrual dayBefore = AnnualLeavePolicy.accrualAsOf(LocalDate.of(1990, 1, 1),
				LocalDate.of(2025, 6, 15), LocalDate.of(2026, 6, 14));
		AnnualLeavePolicy.Accrual anniversary = AnnualLeavePolicy.accrualAsOf(LocalDate.of(1990, 1, 1),
				LocalDate.of(2025, 6, 15), LocalDate.of(2026, 6, 15));

		assertThat(dayBefore.earnedDays()).isZero();
		assertThat(anniversary.earnedDays()).isEqualTo(14);
	}

	@Test
	void addsUpEveryCompletedServiceYear() {
		// hired 2019-03-01, as of 2026-06-15: 7 completed years -> 5 x 14 + 2 x 20
		AnnualLeavePolicy.Accrual accrual = AnnualLeavePolicy.accrualAsOf(LocalDate.of(1990, 1, 1),
				LocalDate.of(2019, 3, 1), LocalDate.of(2026, 6, 15));

		assertThat(accrual.completedServiceYears()).isEqualTo(7);
		assertThat(accrual.earnedDays()).isEqualTo(5 * 14 + 2 * 20);
		assertThat(accrual.lastEntitlementDays()).isEqualTo(20);
		assertThat(accrual.nextAccrualDate()).isEqualTo(LocalDate.of(2027, 3, 1));
	}

	@Test
	void appliesTheAgeFloorPerAnniversary() {
		// born 1974-01-01, hired 2022-06-01: turns 50 on 2024-01-01
		// anniversaries: 2023-06-01 (age 49) -> 14, 2024-06-01 (age 50) -> 20, 2025-06-01 (age 51) -> 20
		AnnualLeavePolicy.Accrual accrual = AnnualLeavePolicy.accrualAsOf(LocalDate.of(1974, 1, 1),
				LocalDate.of(2022, 6, 1), LocalDate.of(2026, 3, 1));

		assertThat(accrual.earnedDays()).isEqualTo(14 + 20 + 20);
	}

}
