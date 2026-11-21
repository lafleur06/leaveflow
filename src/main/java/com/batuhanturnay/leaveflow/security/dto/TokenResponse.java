package com.batuhanturnay.leaveflow.security.dto;

/** Send {@code accessToken} as {@code Authorization: Bearer <token>} on every request. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

}
