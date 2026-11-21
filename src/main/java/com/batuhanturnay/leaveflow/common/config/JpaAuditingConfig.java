package com.batuhanturnay.leaveflow.common.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kept out of the main application class so that sliced tests
 * (for example {@code @WebMvcTest}) do not need a JPA context.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
class JpaAuditingConfig {

	/**
	 * PostgreSQL stores timestamps with microsecond precision. Truncating here keeps the
	 * value returned right after a save identical to the one read back from the database.
	 */
	@Bean
	DateTimeProvider auditingDateTimeProvider() {
		return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS));
	}

}
