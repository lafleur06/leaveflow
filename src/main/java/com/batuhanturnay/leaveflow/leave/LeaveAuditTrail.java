package com.batuhanturnay.leaveflow.leave;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes every {@link LeaveStatusChanged} to {@code leave_request_events}.
 * <p>
 * A plain (synchronous) listener on purpose: it runs inside the transaction that changed the
 * request, so the change and its history row are committed together or not at all.
 */
@Component
class LeaveAuditTrail {

	private final LeaveRequestEventRepository eventRepository;

	LeaveAuditTrail(LeaveRequestEventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@EventListener
	void record(LeaveStatusChanged change) {
		eventRepository.save(new LeaveRequestEvent(change));
	}

}
