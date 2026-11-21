package com.batuhanturnay.leaveflow.leave;

import java.util.EnumSet;
import java.util.Set;

/**
 * <pre>
 *            approve            cancel (before start)
 * PENDING ───────────► APPROVED ─────────────────► CANCELLED
 *    │  │
 *    │  └─ reject ──► REJECTED
 *    └──── cancel ──► CANCELLED
 * </pre>
 */
public enum LeaveStatus {

	PENDING, APPROVED, REJECTED, CANCELLED;

	/** Statuses that hold days in the calendar and, for annual leave, in the balance. */
	public static final Set<LeaveStatus> ACTIVE = EnumSet.of(PENDING, APPROVED);

}
