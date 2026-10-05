package com.rauldev.personalfinance.entry.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryQueryPort;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.DashboardQueryPort;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.port.out.TransferOperationRepository;
import com.rauldev.personalfinance.application.port.out.UserQueryPort;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcAccountQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcAccountRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcCategoryQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcCategoryRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcDashboardQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcExpenseOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcFinancialOperationQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcIncomeOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcObligationQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcObligationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcOccurrenceResolutionRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcReversalRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcTransferOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcUserQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class PersistenceConfigurationTest {

    private static final String URL_PROPERTY = "personal-finance.sqlite.url=";

    @TempDir
    Path tempDir;

    private final ApplicationContextRunner runner =
        new ApplicationContextRunner().withUserConfiguration(PersistenceConfiguration.class);

    @Test
    void failsStartupWhenUrlIsMissing() {
        runner.run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCauseMessage(context.getStartupFailure()).contains("SQLite URL is required"));
        });
    }

    @Test
    void failsStartupWhenUrlIsBlank() {
        runner.withPropertyValues(URL_PROPERTY + "   ").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCauseMessage(context.getStartupFailure()).contains("SQLite URL is required"));
        });
    }

    @Test
    void failsStartupWhenUrlIsProvidedUnderWrongPrefix() {
        runner.withPropertyValues("sqlite.url=jdbc:sqlite:" + tempDir.resolve("finance.db")).run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCauseMessage(context.getStartupFailure()).contains("SQLite URL is required"));
        });
    }

    @Test
    void failsStartupWhenUrlIsNotJdbcSqlite() {
        runner.withPropertyValues(URL_PROPERTY + "jdbc:postgresql://localhost/finance").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(rootCauseMessage(context.getStartupFailure())
                .contains("SQLite URL must start with 'jdbc:sqlite:'"));
        });
    }

    @Test
    void exposesPersistenceBeansWithoutCreatingDatabaseFile() {
        Path dbPath = tempDir.resolve("finance.db");

        runner.withPropertyValues(URL_PROPERTY + "jdbc:sqlite:" + dbPath).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(SQLiteConnectionProvider.class).size());
            assertEquals(1, context.getBeansOfType(TransactionConnectionHolder.class).size());
            assertInstanceOf(JdbcTransactionManager.class, context.getBean(TransactionManager.class));
            assertInstanceOf(JdbcUserQueryAdapter.class, context.getBean(UserQueryPort.class));
            assertInstanceOf(JdbcAccountRepository.class, context.getBean(AccountRepository.class));
            assertInstanceOf(JdbcCategoryRepository.class, context.getBean(CategoryRepository.class));
            assertInstanceOf(JdbcAccountQueryAdapter.class, context.getBean(AccountQueryPort.class));
            assertInstanceOf(JdbcCategoryQueryAdapter.class, context.getBean(CategoryQueryPort.class));
            assertInstanceOf(JdbcIncomeOperationRepository.class, context.getBean(IncomeOperationRepository.class));
            assertInstanceOf(JdbcExpenseOperationRepository.class, context.getBean(ExpenseOperationRepository.class));
            assertInstanceOf(JdbcTransferOperationRepository.class,
                context.getBean(TransferOperationRepository.class));
            assertInstanceOf(JdbcReversalRepository.class, context.getBean(ReversalRepository.class));
            assertInstanceOf(JdbcObligationRepository.class, context.getBean(ObligationRepository.class));
            assertInstanceOf(JdbcObligationQueryAdapter.class, context.getBean(ObligationQueryPort.class));
            assertInstanceOf(JdbcOccurrenceResolutionRepository.class,
                context.getBean(OccurrenceResolutionRepository.class));
            assertInstanceOf(JdbcFinancialOperationQueryAdapter.class,
                context.getBean(FinancialOperationQueryPort.class));
            assertInstanceOf(JdbcDashboardQueryAdapter.class, context.getBean(DashboardQueryPort.class));
        });

        assertFalse(dbPath.toFile().exists());
    }

    @Test
    void transactionManagerBeanCommitsSuccessfulWork() {
        Path dbPath = tempDir.resolve("finance.db");
        UUID userId = UUID.randomUUID();

        runner.withPropertyValues(URL_PROPERTY + "jdbc:sqlite:" + dbPath).run(context -> {
            initializeSchema(context.getBean(SQLiteConnectionProvider.class));
            TransactionManager transactionManager = context.getBean(TransactionManager.class);
            TransactionConnectionHolder holder = context.getBean(TransactionConnectionHolder.class);

            transactionManager.execute(() -> insertUser(holder.get(), userId));

            assertEquals(1, countUsers(context.getBean(SQLiteConnectionProvider.class), userId));
        });
    }

    @Test
    void userQueryPortBeanSeesRowsOfTheActiveTransaction() {
        Path dbPath = tempDir.resolve("finance.db");
        UUID userId = UUID.randomUUID();

        runner.withPropertyValues(URL_PROPERTY + "jdbc:sqlite:" + dbPath).run(context -> {
            initializeSchema(context.getBean(SQLiteConnectionProvider.class));
            TransactionManager transactionManager = context.getBean(TransactionManager.class);
            TransactionConnectionHolder holder = context.getBean(TransactionConnectionHolder.class);
            UserQueryPort userQueryPort = context.getBean(UserQueryPort.class);
            boolean[] seen = new boolean[1];

            transactionManager.execute(() -> {
                insertUser(holder.get(), userId);
                seen[0] = userQueryPort.existsById(userId);
            });

            assertTrue(seen[0]);
        });
    }

    @Test
    void transactionManagerBeanRollsBackFailedWork() {
        Path dbPath = tempDir.resolve("finance.db");
        UUID userId = UUID.randomUUID();

        runner.withPropertyValues(URL_PROPERTY + "jdbc:sqlite:" + dbPath).run(context -> {
            initializeSchema(context.getBean(SQLiteConnectionProvider.class));
            TransactionManager transactionManager = context.getBean(TransactionManager.class);
            TransactionConnectionHolder holder = context.getBean(TransactionConnectionHolder.class);

            assertThrows(IllegalStateException.class, () -> transactionManager.execute(() -> {
                insertUser(holder.get(), userId);
                throw new IllegalStateException("Simulated business error");
            }));

            assertEquals(0, countUsers(context.getBean(SQLiteConnectionProvider.class), userId));
            assertFalse(holder.hasActiveTransaction());
        });
    }

    private static String rootCauseMessage(Throwable failure) {
        Throwable current = failure;
        StringBuilder messages = new StringBuilder();
        while (current != null) {
            messages.append(current.getMessage()).append('\n');
            current = current.getCause();
        }
        return messages.toString();
    }

    private static void insertUser(Connection connection, UUID userId) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("INSERT INTO users (id) VALUES ('" + userId + "')");
        } catch (java.sql.SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static int countUsers(SQLiteConnectionProvider provider, UUID userId) throws Exception {
        try (Connection conn = provider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE id = '" + userId + "'")) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private static void initializeSchema(SQLiteConnectionProvider provider) throws Exception {
        try (Connection connection = provider.getConnection();
             InputStream is = PersistenceConfigurationTest.class.getClassLoader()
                 .getResourceAsStream("db/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("db/schema.sql resource not found on classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        }
    }
}
