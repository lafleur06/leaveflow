package com.batuhanturnay.leaveflow.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code leaveflow.security}. Secrets come from environment variables
 * ({@code LEAVEFLOW_JWT_SECRET}, {@code LEAVEFLOW_ADMIN_PASSWORD}), never from the repository.
 *
 * @param jwtSecret HMAC key for signing tokens, at least 32 bytes; when empty a random key is
 * generated at startup (fine for development, but tokens stop working after a restart)
 * @param tokenTtl how long an access token stays valid
 * @param admin the first HR account, created at startup when no HR account exists yet
 */
@ConfigurationProperties("leaveflow.security")
public record SecurityProperties(String jwtSecret, @DefaultValue("1h") Duration tokenTtl,
		@DefaultValue Admin admin) {

	/**
	 * @param email login of the bootstrap HR account; leave empty to disable the bootstrap
	 * @param password its initial password; when empty a random one is generated and logged once
	 */
	public record Admin(String email, String password) {

	}

}
