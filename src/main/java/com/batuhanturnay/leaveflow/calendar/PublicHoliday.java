package com.batuhanturnay.leaveflow.calendar;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.batuhanturnay.leaveflow.common.persistence.BaseEntity;

@Entity
@Table(name = "public_holidays")
public class PublicHoliday extends BaseEntity {

	@Column(name = "holiday_date", nullable = false, unique = true)
	private LocalDate date;

	@Column(nullable = false, length = 100)
	private String name;

	protected PublicHoliday() {
		// for JPA
	}

	public PublicHoliday(LocalDate date, String name) {
		this.date = date;
		this.name = name;
	}

	public LocalDate getDate() {
		return date;
	}

	public String getName() {
		return name;
	}

}
