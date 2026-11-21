package com.batuhanturnay.leaveflow.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HR sets an initial password or resets a forgotten one. BCrypt uses at most 72 bytes. */
public record SetPasswordRequest(@NotBlank @Size(min = 8, max = 72) String newPassword) {

}
