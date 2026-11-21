package com.batuhanturnay.leaveflow.common.error;

/** The request clashes with the current state of the data (duplicates, dependent records). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}

}
