package com.batuhanturnay.leaveflow.common.error;

/** The credentials are wrong or missing. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}

}
