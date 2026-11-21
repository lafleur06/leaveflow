package com.batuhanturnay.leaveflow.leave;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import com.batuhanturnay.leaveflow.employee.Employee;

/**
 * One entry in a leave request's history. Append-only: Hibernate never updates it
 * ({@link Immutable}) and nothing in the application deletes it.
 */
@Entity
@Immutable
@Table(name = "leave_request_events")
public class LeaveRequestEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "leave_request_id", nullable = false, updatable = false)
	private LeaveRequest leaveRequest;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20, updatable = false)
	private LeaveAction action;

	/** Empty only for cancellations recorded before the audit trail existed. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "actor_id", updatable = false)
	private Employee actor;

	@Column(length = 500, updatable = false)
	private String note;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected LeaveRequestEvent() {
		// for JPA
	}

	LeaveRequestEvent(LeaveStatusChanged change) {
		this.leaveRequest = change.request();
		this.action = change.action();
		this.actor = change.actor();
		this.note = change.note();
		this.occurredAt = change.occurredAt();
	}

	public Long getId() {
		return id;
	}

	public LeaveAction getAction() {
		return action;
	}

	public Employee getActor() {
		return actor;
	}

	public String getNote() {
		return note;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

}
