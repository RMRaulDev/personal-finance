package com.rauldev.personalfinance.entry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point.
 *
 * <p>Lives in the {@code entry} package so component scanning is limited to the entry layer.
 * Domain, application, and infrastructure classes are never scanned; they are wired explicitly
 * through {@code @Bean} methods in {@code entry.config}.
 */
@SpringBootApplication
public class PersonalFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalFinanceApplication.class, args);
    }
}
