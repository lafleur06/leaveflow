package com.batuhanturnay.leaveflow.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BCrypt only uses the first 72 bytes of a password, hence the upper limit. */
public record ChangePasswordRequest(

		@NotBlank String currentPassword,

		@NotBlank @Size(min = 8, max = 72) String newPassword) {

}
