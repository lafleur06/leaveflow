package com.batuhanturnay.leaveflow.leave;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.batuhanturnay.leaveflow.common.error.ConflictException;
import com.batuhanturnay.leaveflow.common.persistence.BaseEntity;
import com.batuhanturnay.leaveflow.employee.Employee;

/**
 * A request for time off. The status only changes through {@link #approve}, {@link #reject}
 * and {@link #cancel}, which enforce the allowed transitions (see {@link LeaveStatus}).
 */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveType type;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "working_days", nullable = false)
	private int workingDays;

	@Column(length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "decided_by_id")
	private Employee decidedBy;

	@Column(name = "decided_at")
	private Instant decidedAt;

	@Column(name = "decision_note", length = 500)
	private String decisionNote;

	@Version
	@Column(nullable = false)
	private long version;

	protected LeaveRequest() {
		// for JPA
	}

	public LeaveRequest(Employee employee, LeaveType type, LocalDate startDate, LocalDate endDate, int workingDays,
			String reason) {
		this.employee = employee;
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
		this.workingDays = workingDays;
		this.reason = reason;
		this.status = LeaveStatus.PENDING;
	}

	public void approve(Employee approver, String note, Instant now) {
		decide(LeaveStatus.APPROVED, approver, note, now);
	}

	public void reject(Employee approver, String note, Instant now) {
		decide(LeaveStatus.REJECTED, approver, note, now);
	}

	/**
	 * A pending request can always be withdrawn. An approved one only before it starts:
	 * leave that has begun (or is over) is part of the record.
	 */
	public void cancel(LocalDate today) {
		boolean allowed = status == LeaveStatus.PENDING
				|| (status == LeaveStatus.APPROVED && startDate.isAfter(today));
		if (!allowed) {
			throw new ConflictException(status == LeaveStatus.APPROVED
					? "Leave request %d has already started and can no longer be cancelled".formatted(getId())
					: "Leave request %d is %s and cannot be cancelled".formatted(getId(), status));
		}
		status = LeaveStatus.CANCELLED;
	}

	private void decide(LeaveStatus outcome, Employee approver, String note, Instant now) {
		if (status != LeaveStatus.PENDING) {
			throw new ConflictException(
					"Leave request %d is already %s; only PENDING requests can be decided".formatted(getId(), status));
		}
		this.status = outcome;
		this.decidedBy = approver;
		this.decisionNote = note;
		this.decidedAt = now;
	}

	public Employee getEmployee() {
		return employee;
	}

	public LeaveType getType() {
		return type;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public int getWorkingDays() {
		return workingDays;
	}

	public String getReason() {
		return reason;
	}

	public LeaveStatus getStatus() {
		return status;
	}

	public Employee getDecidedBy() {
		return decidedBy;
	}

	public Instant getDecidedAt() {
		return decidedAt;
	}

	public String getDecisionNote() {
		return decisionNote;
	}

}
