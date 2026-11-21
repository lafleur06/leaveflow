package com.batuhanturnay.leaveflow.calendar.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HolidayRequest(

		@NotNull LocalDate date,

		@NotBlank @Size(max = 100) String name) {

}
