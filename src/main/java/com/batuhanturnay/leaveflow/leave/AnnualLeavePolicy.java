package com.batuhanturnay.leaveflow.leave;

import java.time.LocalDate;
import java.time.Period;

/**
 * Annual paid leave entitlement under Turkish Labour Law No. 4857, Art. 53.
 * <ul>
 * <li>1 to 5 years of service (5 included): 14 working days</li>
 * <li>more than 5 and less than 15 years: 20 working days</li>
 * <li>15 years or more: 26 working days</li>
 * <li>employees aged 18 or younger, or 50 or older: never less than 20 days</li>
 * </ul>
 * Leave is earned on each anniversary of the hire date; nothing is earned during the first year.
 * Pure calculation with no database access, so it can be unit-tested directly.
 */
public final class AnnualLeavePolicy {

	private AnnualLeavePolicy() {
	}

	/**
	 * @param serviceYear the completed year of service that is being rewarded (1 = first anniversary)
	 * @param ageOnAnniversary the employee's age on that anniversary
	 */
	public static int entitlementFor(int serviceYear, int ageOnAnniversary) {
		if (serviceYear < 1) {
			return 0;
		}
		int days;
		if (serviceYear <= 5) {
			days = 14;
		}
		else if (serviceYear < 15) {
			days = 20;
		}
		else {
			days = 26;
		}
		if (ageOnAnniversary <= 18 || ageOnAnniversary >= 50) {
			days = Math.max(days, 20);
		}
		return days;
	}

	/** Everything earned from the hire date up to and including {@code today}. */
	public static Accrual accrualAsOf(LocalDate birthDate, LocalDate hireDate, LocalDate today) {
		int completedYears = today.isBefore(hireDate) ? 0 : Period.between(hireDate, today).getYears();
		int earned = 0;
		int lastEntitlement = 0;
		for (int year = 1; year <= completedYears; year++) {
			lastEntitlement = entitlementFor(year, ageOn(birthDate, hireDate.plusYears(year)));
			earned += lastEntitlement;
		}
		LocalDate nextAnniversary = hireDate.plusYears(completedYears + 1);
		int nextEntitlement = entitlementFor(completedYears + 1, ageOn(birthDate, nextAnniversary));
		return new Accrual(completedYears, earned, lastEntitlement, nextAnniversary, nextEntitlement);
	}

	private static int ageOn(LocalDate birthDate, LocalDate day) {
		return Period.between(birthDate, day).getYears();
	}

	/**
	 * @param completedServiceYears whole years since the hire date
	 * @param earnedDays total days earned on all past anniversaries
	 * @param lastEntitlementDays days earned on the most recent anniversary (0 in the first year)
	 * @param nextAccrualDate the next anniversary, when new leave is earned
	 * @param nextAccrualDays days that will be earned on that anniversary
	 */
	public record Accrual(int completedServiceYears, int earnedDays, int lastEntitlementDays,
			LocalDate nextAccrualDate, int nextAccrualDays) {

	}

}
