package com.batuhanturnay.leaveflow.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Tokens are signed and verified with one shared HMAC-SHA256 key. The application is both the
 * issuer (login) and the resource server (every other endpoint), so a symmetric key is enough;
 * Spring Security's own Nimbus support is used instead of a separate JWT library.
 */
@Configuration(proxyBeanMethods = false)
class JwtConfig {

	private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

	private static final int MIN_KEY_BYTES = 32;

	@Bean
	SecretKey jwtSigningKey(SecurityProperties properties) {
		byte[] key;
		if (properties.jwtSecret() == null || properties.jwtSecret().isBlank()) {
			log.warn("leaveflow.security.jwt-secret is not set: using a random signing key. "
					+ "Tokens will stop working after a restart. Set LEAVEFLOW_JWT_SECRET in production.");
			key = new byte[MIN_KEY_BYTES];
			new SecureRandom().nextBytes(key);
		}
		else {
			key = properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
			if (key.length < MIN_KEY_BYTES) {
				throw new IllegalStateException(
						"leaveflow.security.jwt-secret must be at least %d bytes long".formatted(MIN_KEY_BYTES));
			}
		}
		return new SecretKeySpec(key, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
		return NimbusJwtEncoder.withSecretKey(jwtSigningKey).build();
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** BCrypt by default; stored hashes carry an {@code {id}} prefix so the algorithm can change later. */
	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
