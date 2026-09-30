package com.rauldev.personalfinance.entry;

import java.nio.file.Path;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

@SpringBootTest
class PersonalFinanceApplicationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteUrl(DynamicPropertyRegistry registry) {
        registry.add("personal-finance.sqlite.url",
            () -> "jdbc:sqlite:" + tempDir.resolve("startup.db").toAbsolutePath());
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void contextContainsPersistenceBeans() {
        assertEquals(1, context.getBeansOfType(SQLiteConnectionProvider.class).size());
        assertEquals(1, context.getBeansOfType(TransactionConnectionHolder.class).size());
        assertInstanceOf(JdbcTransactionManager.class, context.getBean(TransactionManager.class));
    }

    @Test
    void contextHasNoCurrentUserProviderBean() {
        assertEquals(0, context.getBeanNamesForType(CurrentUserProvider.class).length);
    }

    @Test
    void contextHasNoDataSourceBean() {
        assertEquals(0, context.getBeanNamesForType(DataSource.class).length);
    }

    @Test
    void startupDoesNotCreateDatabaseFile() {
        assertFalse(tempDir.resolve("startup.db").toFile().exists());
    }

    @Test
    void onlyExplicitlyWiredCoreClassesAreBeans() {
        Set<Class<?>> allowed = Set.of(
            SQLiteConnectionProvider.class, TransactionConnectionHolder.class, JdbcTransactionManager.class);

        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null || allowed.contains(type)) {
                continue;
            }
            String pkg = type.getPackageName();
            for (String layer : List.of("domain", "application", "infrastructure")) {
                assertFalse(pkg.startsWith("com.rauldev.personalfinance." + layer),
                    "Unexpected core bean '" + name + "' of type " + type.getName());
            }
        }
    }
}
