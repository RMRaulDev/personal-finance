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

import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcAccountQueryAdapterTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcAccountQueryAdapter accountQueryAdapter;

    private static final UUID USER_A_ID = UUID.randomUUID();
    private static final UUID USER_B_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        accountQueryAdapter = new JdbcAccountQueryAdapter(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_A_ID);
            seedUser(conn, USER_B_ID);
        }
    }

    @Test
    void findByIdAndUserId_returnsAccountDetailsWhenAccountExists() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account account = new Account(accountId, USER_A_ID, "Checking");
            account.credit(Money.of("150.00"));
            accountRepository.create(account);
            accountRepository.update(account);
        });

        Optional<AccountDetails> details = accountQueryAdapter.findByIdAndUserId(accountId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(accountId, details.get().id());
        assertEquals(USER_A_ID, details.get().userId());
        assertEquals("Checking", details.get().name());
        assertEquals(Money.of("150.00"), details.get().balance());
        assertEquals(AccountStatus.ACTIVE, details.get().status());
    }

    @Test
    void findByIdAndUserId_returnsEmptyWhenAccountDoesNotExist() {
        Optional<AccountDetails> details = accountQueryAdapter.findByIdAndUserId(UUID.randomUUID(), USER_A_ID);

        assertFalse(details.isPresent());
    }

    @Test
    void findByIdAndUserId_returnsEmptyWhenUserIdDoesNotMatch() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account account = new Account(accountId, USER_A_ID, "Savings");
            accountRepository.create(account);
        });

        Optional<AccountDetails> details = accountQueryAdapter.findByIdAndUserId(accountId, USER_B_ID);

        assertFalse(details.isPresent());
    }

    @Test
    void findByIdAndUserId_mapsZeroAndPositiveBalancesCorrectly() {
        UUID zeroBalanceAccountId = UUID.randomUUID();
        UUID positiveBalanceAccountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account zeroAccount = new Account(zeroBalanceAccountId, USER_A_ID, "Zero Account");
            accountRepository.create(zeroAccount);

            Account positiveAccount = new Account(positiveBalanceAccountId, USER_A_ID, "Positive Account");
            positiveAccount.credit(Money.of("99.99"));
            accountRepository.create(positiveAccount);
            accountRepository.update(positiveAccount);
        });

        Optional<AccountDetails> zeroDetails = accountQueryAdapter.findByIdAndUserId(zeroBalanceAccountId, USER_A_ID);
        Optional<AccountDetails> positiveDetails = accountQueryAdapter.findByIdAndUserId(positiveBalanceAccountId, USER_A_ID);

        assertTrue(zeroDetails.isPresent());
        assertEquals(Money.ofCents(0), zeroDetails.get().balance());

        assertTrue(positiveDetails.isPresent());
        assertEquals(Money.of("99.99"), positiveDetails.get().balance());
    }

    @Test
    void findByIdAndUserId_mapsInactiveAccountStatusCorrectly() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account account = new Account(accountId, USER_A_ID, "Inactive Account");
            account.deactivate();
            accountRepository.create(account);
            accountRepository.update(account);
        });

        Optional<AccountDetails> details = accountQueryAdapter.findByIdAndUserId(accountId, USER_A_ID);

        assertTrue(details.isPresent());
        assertEquals(AccountStatus.INACTIVE, details.get().status());
    }

    @Test
    void findByIdAndUserId_worksBothOutsideAndInsideActiveTransaction() {
        UUID accountId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Account account = new Account(accountId, USER_A_ID, "Transaction Test Account");
            accountRepository.create(account);
        });

        Optional<AccountDetails> outsideTxDetails = accountQueryAdapter.findByIdAndUserId(accountId, USER_A_ID);
        assertTrue(outsideTxDetails.isPresent());
        assertEquals("Transaction Test Account", outsideTxDetails.get().name());

        Optional<AccountDetails> insideTxDetails = transactionManager.execute(
            () -> accountQueryAdapter.findByIdAndUserId(accountId, USER_A_ID));
        assertTrue(insideTxDetails.isPresent());
        assertEquals("Transaction Test Account", insideTxDetails.get().name());
    }

    @Test
    void findByIdAndUserId_rejectsNullParameters() {
        assertThrows(NullPointerException.class,
            () -> accountQueryAdapter.findByIdAndUserId(null, USER_A_ID));

        assertThrows(NullPointerException.class,
            () -> accountQueryAdapter.findByIdAndUserId(UUID.randomUUID(), null));
    }

    @Test
    void failsWithCorruptedPersistedDataWhenAccountStatusIsInvalid() throws SQLException {
        UUID rowId = UUID.randomUUID();
        insertRawRow("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + rowId + "', '" + USER_A_ID + "', 'Broken', 0, 'BOGUS')");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class, () -> accountQueryAdapter.findByIdAndUserId(rowId, USER_A_ID));

        assertTrue(ex.getMessage().contains("accounts"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void findByUserId_returnsAccountsOrderedByNameThenId() {
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "Savings", 300, "ACTIVE");
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "Checking", 200, "ACTIVE");
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "Wallet", 100, "ACTIVE");

        List<AccountDetails> accounts = accountQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of("Checking", "Savings", "Wallet"), accounts.stream().map(AccountDetails::name).toList());
    }

    @Test
    void findByUserId_ordersByNameWithBinaryCollationUppercaseBeforeLowercase() {
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "alpha", 0, "ACTIVE");
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "Zeta", 0, "ACTIVE");

        List<AccountDetails> accounts = accountQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of("Zeta", "alpha"), accounts.stream().map(AccountDetails::name).toList());
    }

    @Test
    void findByUserId_mapsEveryFieldIncludingBalanceAndBothStatuses() {
        UUID activeId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        insertAccountRow(activeId, USER_A_ID, "Active", 12345, "ACTIVE");
        insertAccountRow(inactiveId, USER_A_ID, "Inactive", 0, "INACTIVE");

        List<AccountDetails> accounts = accountQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(List.of(
            new AccountDetails(activeId, USER_A_ID, "Active", Money.ofCents(12345), AccountStatus.ACTIVE),
            new AccountDetails(inactiveId, USER_A_ID, "Inactive", Money.ofCents(0), AccountStatus.INACTIVE)),
            accounts);
    }

    @Test
    void findByUserId_excludesOtherUsersAccounts() {
        UUID ownId = UUID.randomUUID();
        insertAccountRow(ownId, USER_A_ID, "Mine", 0, "ACTIVE");
        insertAccountRow(UUID.randomUUID(), USER_B_ID, "Theirs", 0, "ACTIVE");

        List<AccountDetails> accounts = accountQueryAdapter.findByUserId(USER_A_ID);

        assertEquals(1, accounts.size());
        assertEquals(ownId, accounts.get(0).id());
    }

    @Test
    void findByUserId_returnsEmptyListWhenUserHasNoAccounts() {
        insertAccountRow(UUID.randomUUID(), USER_B_ID, "Theirs", 0, "ACTIVE");

        assertEquals(List.of(), accountQueryAdapter.findByUserId(USER_A_ID));
    }

    @Test
    void findByUserId_worksOutsideAndInsideActiveTransaction() {
        insertAccountRow(UUID.randomUUID(), USER_A_ID, "Wallet", 0, "ACTIVE");

        List<AccountDetails> outside = accountQueryAdapter.findByUserId(USER_A_ID);
        List<AccountDetails> inside = transactionManager.execute(() -> accountQueryAdapter.findByUserId(USER_A_ID));

        assertEquals(1, outside.size());
        assertEquals(outside, inside);
    }

    @Test
    void findByUserId_insideTransactionReusesBoundConnectionWithoutClosingIt() {
        UUID uncommitted = UUID.randomUUID();

        transactionManager.execute(() -> {
            Connection bound = connectionHolder.get();
            try (Statement stmt = bound.createStatement()) {
                stmt.executeUpdate("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('"
                    + uncommitted + "', '" + USER_A_ID + "', 'Pending', 0, 'ACTIVE')");

                List<AccountDetails> accounts = accountQueryAdapter.findByUserId(USER_A_ID);

                assertEquals(1, accounts.size());
                assertEquals(uncommitted, accounts.get(0).id());
                assertFalse(bound.isClosed());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void findByUserId_rejectsNullUserId() {
        assertThrows(NullPointerException.class, () -> accountQueryAdapter.findByUserId(null));
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenStatusIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertAccountRow(rowId, USER_A_ID, "Broken", 0, "BOGUS");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> accountQueryAdapter.findByUserId(USER_A_ID));

        assertTrue(ex.getMessage().contains("accounts"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenIdIsNotAUuid() {
        insertAccountRow("not-a-uuid", USER_A_ID, "Broken", 0, "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> accountQueryAdapter.findByUserId(USER_A_ID));

        assertTrue(ex.getMessage().contains("accounts"));
        assertTrue(ex.getMessage().contains("not-a-uuid"));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    private void insertAccountRow(Object id, UUID userId, String name, long balance, String status) {
        try {
            insertRawRow("INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + id + "', '"
                + userId + "', '" + name + "', " + balance + ", '" + status + "')");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
