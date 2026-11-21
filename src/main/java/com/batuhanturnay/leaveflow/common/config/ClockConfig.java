package com.batuhanturnay.leaveflow.common.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "Today" matters for leave rules (balances, past dates, cancellation), so it comes from an
 * injectable {@link Clock} instead of {@code LocalDate.now()}. Tests replace it with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

	static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Istanbul");

	@Bean
	Clock clock() {
		return Clock.system(BUSINESS_ZONE);
	}

}
