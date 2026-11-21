package com.batuhanturnay.leaveflow.employee.dto;

import jakarta.validation.constraints.NotNull;

import com.batuhanturnay.leaveflow.employee.Role;

public record RoleChangeRequest(@NotNull Role role) {

}
