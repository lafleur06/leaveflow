package com.batuhanturnay.leaveflow.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.batuhanturnay.leaveflow.employee.Employee;
import com.batuhanturnay.leaveflow.security.dto.TokenResponse;

@Service
public class TokenService {

	static final String ISSUER = "leaveflow";

	static final String ROLE_CLAIM = "role";

	private final JwtEncoder jwtEncoder;

	private final SecurityProperties properties;

	public TokenService(JwtEncoder jwtEncoder, SecurityProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	/**
	 * The subject is the employee id; role, email and name are copied into the token so the
	 * API does not need a database lookup to know who is calling.
	 * Uses the real current time, not the business {@code Clock}: token validation is always
	 * done against real time.
	 */
	public TokenResponse issue(Employee employee) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(employee.getId().toString())
			.issuedAt(now)
			.expiresAt(now.plus(properties.tokenTtl()))
			.claim(ROLE_CLAIM, employee.getRole().name())
			.claim("email", employee.getEmail())
			.claim("name", employee.getFullName())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", properties.tokenTtl().toSeconds());
	}

}
