package com.batuhanturnay.leaveflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.batuhanturnay.leaveflow.employee.Role;

/** Reads the caller from the security context, for permission checks inside services. */
@Component
public class CurrentUser {

	public AuthenticatedUser get() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken token) {
			Jwt jwt = token.getToken();
			return new AuthenticatedUser(Long.parseLong(jwt.getSubject()),
					Role.valueOf(jwt.getClaimAsString(TokenService.ROLE_CLAIM)));
		}
		throw new IllegalStateException("No authenticated user in the security context");
	}

}
