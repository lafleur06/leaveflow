package com.batuhanturnay.leaveflow.leave;

public enum LeaveType {

	/** Annual paid leave: uses the employee's earned balance and must be planned ahead. */
	ANNUAL(true, false),

	/** Sick leave backed by a medical report: often submitted afterwards, so past dates are allowed. */
	SICK(false, true),

	/** Unpaid leave: does not touch the annual balance. */
	UNPAID(false, false);

	private final boolean deductsFromBalance;

	private final boolean allowsPastDates;

	LeaveType(boolean deductsFromBalance, boolean allowsPastDates) {
		this.deductsFromBalance = deductsFromBalance;
		this.allowsPastDates = allowsPastDates;
	}

	public boolean deductsFromBalance() {
		return deductsFromBalance;
	}

	public boolean allowsPastDates() {
		return allowsPastDates;
	}

}
