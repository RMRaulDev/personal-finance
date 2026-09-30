package com.rauldev.personalfinance.entry.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

/**
 * Wires the JDBC persistence and transaction infrastructure.
 *
 * <p>None of these beans opens a database connection when it is created; connections are only
 * opened per transaction or query. Repositories, query adapters, and use cases are wired here
 * once an endpoint needs them.
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
}
