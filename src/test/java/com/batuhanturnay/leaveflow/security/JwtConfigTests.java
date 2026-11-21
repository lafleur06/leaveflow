package com.batuhanturnay.leaveflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

class JwtConfigTests {

	private final JwtConfig config = new JwtConfig();

	@Test
	void usesTheConfiguredSecret() {
		String secret = "a-secret-that-is-at-least-32-bytes-long";

		SecretKey key = config.jwtSigningKey(withSecret(secret));

		assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
		assertThat(key.getEncoded()).isEqualTo(secret.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void refusesASecretShorterThan32Bytes() {
		assertThatIllegalStateException().isThrownBy(() -> config.jwtSigningKey(withSecret("too-short")))
			.withMessageContaining("at least 32 bytes");
	}

	@Test
	void generatesADifferentRandomKeyOnEachStartWithoutASecret() {
		SecretKey first = config.jwtSigningKey(withSecret(""));
		SecretKey second = config.jwtSigningKey(withSecret(null));

		assertThat(first.getEncoded()).hasSize(32);
		assertThat(first.getEncoded()).isNotEqualTo(second.getEncoded());
	}

	private static SecurityProperties withSecret(String secret) {
		return new SecurityProperties(secret, null, new SecurityProperties.Admin(null, null));
	}

}
