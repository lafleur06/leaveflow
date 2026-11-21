package com.batuhanturnay.leaveflow.common.error;

/** The caller is not allowed to perform this action on this resource. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}

}
