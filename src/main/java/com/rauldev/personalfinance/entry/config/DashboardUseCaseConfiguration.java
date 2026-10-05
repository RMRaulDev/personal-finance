package com.rauldev.personalfinance.entry.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.DashboardQueryPort;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.usecase.GetDashboard;

/**
 * Wires the dashboard use case.
 *
 * <p>The port comes from {@link PersistenceConfiguration} and the {@link Clock} that decides
 * "today" comes from {@link ClockConfiguration}. {@link GetDashboard} is a query, but it receives
 * the {@link TransactionManager} because it runs all its reads in one transaction (a consistent
 * snapshot on a single connection); the {@link DashboardQueryPort} shares the transaction's
 * connection through the same {@code TransactionConnectionHolder} singleton.
 */
@Configuration(proxyBeanMethods = false)
public class DashboardUseCaseConfiguration {

    @Bean
    public GetDashboard getDashboard(
        DashboardQueryPort dashboardQueryPort,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new GetDashboard(dashboardQueryPort, transactionManager, clock);
    }
}
