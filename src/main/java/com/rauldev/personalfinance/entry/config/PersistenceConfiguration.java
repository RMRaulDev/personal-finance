package com.rauldev.personalfinance.entry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.port.out.UserQueryPort;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcUserQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

/**
 * Wires the JDBC persistence and transaction infrastructure.
 *
 * <p>None of these beans opens a database connection when it is created; connections are only
 * opened per transaction or query. Repositories and query adapters are added here once an
 * endpoint needs them; use cases are wired in per-feature configuration classes.
 *
 * <p>Every repository and query adapter receives the same singleton
 * {@link TransactionConnectionHolder} as the {@link TransactionManager}, so they see the
 * connection of the active transaction. Spring injects that singleton because the {@code @Bean}
 * methods declare it as a parameter ({@code proxyBeanMethods = false} means calling another
 * {@code @Bean} method directly would create a new instance instead). Beans are exposed through
 * their application port types.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SqliteProperties.class)
public class PersistenceConfiguration {

    @Bean
    public SQLiteConnectionProvider sqliteConnectionProvider(SqliteProperties sqliteProperties) {
        return new SQLiteConnectionProvider(sqliteProperties.url());
    }

    @Bean
    public TransactionConnectionHolder transactionConnectionHolder() {
        return new TransactionConnectionHolder();
    }

    @Bean
    public TransactionManager transactionManager(
        SQLiteConnectionProvider sqliteConnectionProvider,
        TransactionConnectionHolder transactionConnectionHolder
    ) {
        return new JdbcTransactionManager(sqliteConnectionProvider, transactionConnectionHolder);
    }

    @Bean
    public UserQueryPort userQueryPort(
        SQLiteConnectionProvider sqliteConnectionProvider,
        TransactionConnectionHolder transactionConnectionHolder
    ) {
        return new JdbcUserQueryAdapter(sqliteConnectionProvider, transactionConnectionHolder);
    }
}
