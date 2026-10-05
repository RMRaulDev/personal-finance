package com.rauldev.personalfinance.entry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.UserQueryPort;
import com.rauldev.personalfinance.entry.security.ConfiguredSingleUserProvider;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;

/**
 * Wires how the current user is resolved.
 *
 * <p>No authentication mechanism exists yet, so the application runs in single-user mode: the
 * {@link CurrentUserProvider} bean always returns the configured user. Real authentication will
 * replace only this bean; controllers depend on the {@link CurrentUserProvider} interface and do
 * not change.
 *
 * <p>{@link EnableConfigurationProperties} registers {@link SingleUserProperties} as a bean bound
 * to {@code personal-finance.*}, so it can be received as a {@code @Bean} method parameter.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SingleUserProperties.class)
public class SecurityConfiguration {

    @Bean
    public CurrentUserProvider currentUserProvider(
        SingleUserProperties singleUserProperties,
        UserQueryPort userQueryPort
    ) {
        return new ConfiguredSingleUserProvider(singleUserProperties.userId(), userQueryPort);
    }
}
