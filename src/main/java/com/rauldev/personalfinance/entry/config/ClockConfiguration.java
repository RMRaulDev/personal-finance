package com.rauldev.personalfinance.entry.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link Clock} that use cases receive to compute "today" and timestamps.
 *
 * <p>The clock uses the configured time zone ({@link TimeProperties}), not the server's default
 * zone, so the user's calendar date does not depend on where the server runs.
 *
 * <p>To use a fixed clock in a test: Spring Boot disables bean definition overriding by default,
 * so declaring a second bean named {@code clock} fails startup. Instead, declare the test
 * {@link Clock} bean under a different name and mark it {@code @Primary}, so it wins wherever a
 * {@link Clock} is injected.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TimeProperties.class)
public class ClockConfiguration {

    @Bean
    public Clock clock(TimeProperties timeProperties) {
        return Clock.system(timeProperties.zoneId());
    }
}
