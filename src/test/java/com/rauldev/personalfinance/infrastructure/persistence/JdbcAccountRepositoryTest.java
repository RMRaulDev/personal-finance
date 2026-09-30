package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.sqlite.SQLiteException;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcAccountRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID);
            seedUser(conn, USER_ID_B);
        }
    }

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();

    @Test
    void createsAndFindsAccountById() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account account = new Account(accountId, USER_ID, "Checking");
            accountRepository.create(account);
        });

        Optional<Account> found = transactionManager.execute(() -> accountRepository.findById(accountId));

        assertTrue(found.isPresent());
        assertEquals(accountId, found.get().id());
        assertEquals(USER_ID, found.get().userId());
        assertEquals("Checking", found.get().name());
        assertEquals(Money.ofCents(0), found.get().balance());
        assertEquals(AccountStatus.ACTIVE, found.get().status());
    }

    @Test
    void findsAccountByIdAndCorrectUserId() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> accountRepository.create(new Account(accountId, USER_ID, "Savings")));

        Optional<Account> found = transactionManager.execute(
            () -> accountRepository.findByIdAndUserId(accountId, USER_ID));

        assertTrue(found.isPresent());
        assertEquals(accountId, found.get().id());
    }

    @Test
    void doesNotFindAccountWhenUserIdDoesNotMatch() {
        UUID accountId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        transactionManager.execute(() -> accountRepository.create(new Account(accountId, USER_ID, "Savings")));

        Optional<Account> found = transactionManager.execute(
            () -> accountRepository.findByIdAndUserId(accountId, otherUserId));

        assertFalse(found.isPresent());
    }

    @Test
    void findsAllAccountsForUser() {
        transactionManager.execute(() -> {
            accountRepository.create(new Account(USER_ID, "Checking"));
            accountRepository.create(new Account(USER_ID, "Savings"));
        });

        List<Account> accounts = transactionManager.execute(() -> accountRepository.findByUserId(USER_ID));

        assertEquals(2, accounts.size());
    }

    @Test
    void checksExistenceByUserIdAndName() {
        transactionManager.execute(() -> accountRepository.create(new Account(USER_ID, "Checking")));

        boolean exists = transactionManager.execute(
            () -> accountRepository.existsByUserIdAndName(USER_ID, "Checking"));
        boolean doesNotExist = transactionManager.execute(
            () -> accountRepository.existsByUserIdAndName(USER_ID, "Unknown"));

        assertTrue(exists);
        assertFalse(doesNotExist);
    }

    @Test
    void updatesNameBalanceAndStatus() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> accountRepository.create(new Account(accountId, USER_ID, "Checking")));

        transactionManager.execute(() -> {
            Account account = accountRepository.findById(accountId).orElseThrow();
            account.rename("Renamed");
            account.credit(Money.of("50.00"));
            account.deactivate();
            accountRepository.update(account);
        });

        Optional<Account> updated = transactionManager.execute(() -> accountRepository.findById(accountId));

        assertTrue(updated.isPresent());
        assertEquals("Renamed", updated.get().name());
        assertEquals(Money.of("50.00"), updated.get().balance());
        assertEquals(AccountStatus.INACTIVE, updated.get().status());
    }

    @Test
    void violatesUniquenessConstraintForUserIdAndName() {
        transactionManager.execute(() -> accountRepository.create(new Account(USER_ID, "Checking")));

        assertThrows(RuntimeException.class, () -> transactionManager.execute(
            () -> accountRepository.create(new Account(USER_ID, "Checking"))));
    }

    @Test
    void createAccountWithDuplicateUserIdAndNameThrowsBusinessRuleViolation() {
        transactionManager.execute(() -> accountRepository.create(new Account(USER_ID, "Duplicated")));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
            () -> transactionManager.execute(() -> accountRepository.create(new Account(USER_ID, "Duplicated"))));

        assertEquals(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS, ex.code());
        assertEquals(ApplicationConstants.ACCOUNT_NAME_ALREADY_EXISTS_MESSAGE, ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void createAccountWithDuplicateNameRollsBackTheTransaction() {
        assertThrows(BusinessRuleViolationException.class, () -> transactionManager.execute(() -> {
            accountRepository.create(new Account(USER_ID, "Other"));
            accountRepository.create(new Account(USER_ID, "Duplicated"));
            return accountRepository.create(new Account(USER_ID, "Duplicated"));
        }));

        assertFalse(transactionManager.execute(() -> accountRepository.existsByUserIdAndName(USER_ID, "Other")));
        assertFalse(transactionManager.execute(() -> accountRepository.existsByUserIdAndName(USER_ID, "Duplicated")));
    }

    @Test
    void updateAccountRenamingToAnotherRowsNameThrowsBusinessRuleViolation() {
        UUID firstId = UUID.randomUUID();
        transactionManager.execute(() -> accountRepository.create(new Account(firstId, USER_ID, "First")));
        transactionManager.execute(() -> accountRepository.create(new Account(USER_ID, "Second")));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
            () -> transactionManager.execute(() -> {
                var account = accountRepository.findById(firstId).orElseThrow();
                account.rename("Second");
                return accountRepository.update(account);
            }));

        assertEquals(BusinessRuleCode.ACCOUNT_NAME_ALREADY_EXISTS, ex.code());
        assertEquals(ApplicationConstants.ACCOUNT_NAME_ALREADY_EXISTS_MESSAGE, ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
        assertEquals("First", transactionManager.execute(() -> accountRepository.findById(firstId)).orElseThrow().name());
    }

    @Test
    void createsSameNameForDifferentUsers() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();

        transactionManager.execute(() -> accountRepository.create(new Account(idA, USER_ID, "Shared")));
        transactionManager.execute(() -> accountRepository.create(new Account(idB, USER_ID_B, "Shared")));

        assertEquals(USER_ID, transactionManager.execute(() -> accountRepository.findById(idA)).orElseThrow().userId());
        assertEquals(USER_ID_B, transactionManager.execute(() -> accountRepository.findById(idB)).orElseThrow().userId());
    }

    @Test
    void createAccountWithDuplicateIdStillFailsWithGenericRuntimeException() {
        UUID id = UUID.randomUUID();
        transactionManager.execute(() -> accountRepository.create(new Account(id, USER_ID, "One")));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> accountRepository.create(new Account(id, USER_ID, "Two"))));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create account", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void createAccountWithUnknownUserStillFailsWithGenericRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> accountRepository.create(new Account(UUID.randomUUID(), "Orphan"))));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create account", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> accountRepository.findById(UUID.randomUUID()));
    }

    @Test
    void failsWithCorruptedPersistedDataWhenAccountStatusIsInvalid() throws SQLException {
        UUID rowId = UUID.randomUUID();
        insertRawRow("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + rowId + "', '" + USER_ID + "', 'Broken', 0, 'BOGUS')");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class, () -> transactionManager.execute(() -> accountRepository.findById(rowId)));

        assertTrue(ex.getMessage().contains("accounts"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    private void insertRawRow(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void initializeSchema(Connection connection) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
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

    private void seedUser(Connection connection, UUID userId) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("INSERT INTO users (id) VALUES ('" + userId + "')");
        }
    }
}
