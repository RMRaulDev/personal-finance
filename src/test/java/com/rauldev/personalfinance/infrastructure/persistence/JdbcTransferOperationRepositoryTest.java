package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Transfer;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcTransferOperationRepositoryTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcTransferOperationRepository transferOperationRepository;

    private static final UUID USER_ID_A = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A1 = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A2 = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_B1 = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_B2 = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        transferOperationRepository = new JdbcTransferOperationRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID_A1, USER_ID_A, "Account A1"));
            accountRepository.create(new Account(ACCOUNT_ID_A2, USER_ID_A, "Account A2"));
            accountRepository.create(new Account(ACCOUNT_ID_B1, USER_ID_B, "Account B1"));
            accountRepository.create(new Account(ACCOUNT_ID_B2, USER_ID_B, "Account B2"));
        });
    }

    @Test
    void createsAndFindsTransferById() {
        UUID transferId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);
        Money amount = Money.of("250.00");

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_ID_A, amount, date, ACCOUNT_ID_A1, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        });

        Optional<Transfer> found = transactionManager.execute(() -> transferOperationRepository.findById(transferId));

        assertTrue(found.isPresent());
        assertEquals(transferId, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals(ACCOUNT_ID_A1, found.get().sourceAccountId());
        assertEquals(ACCOUNT_ID_A2, found.get().targetAccountId());
        assertEquals(amount, found.get().amount());
        assertEquals(date, found.get().operationDate());
    }

    @Test
    void reconstructsPersistedFieldsCorrectly() {
        UUID transferId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 8, 15);
        Money amount = Money.of("1234.56");

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_ID_A, amount, date, ACCOUNT_ID_A1, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        });

        Transfer found = transactionManager.execute(() -> transferOperationRepository.findById(transferId).orElseThrow());

        assertEquals(transferId, found.id());
        assertEquals(USER_ID_A, found.userId());
        assertEquals(ACCOUNT_ID_A1, found.sourceAccountId());
        assertEquals(ACCOUNT_ID_A2, found.targetAccountId());
        assertEquals(amount, found.amount());
        assertEquals(date, found.operationDate());
    }

    @Test
    void findsTransferByIdAndCorrectUserId() {
        UUID transferId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A1, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        });

        Optional<Transfer> found = transactionManager.execute(
            () -> transferOperationRepository.findByIdAndUserId(transferId, USER_ID_A));

        assertTrue(found.isPresent());
        assertEquals(transferId, found.get().id());
    }

    @Test
    void doesNotFindTransferWhenUserIdDoesNotMatch() {
        UUID transferId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A1, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        });

        Optional<Transfer> found = transactionManager.execute(
            () -> transferOperationRepository.findByIdAndUserId(transferId, USER_ID_B));

        assertFalse(found.isPresent());
    }

    @Test
    void findsAllTransfersForUser() {
        UUID transferBId = UUID.randomUUID();

        transactionManager.execute(() -> {
            transferOperationRepository.create(
                new Transfer(USER_ID_A, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_ID_A1, ACCOUNT_ID_A2));
            transferOperationRepository.create(
                new Transfer(USER_ID_A, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_A1, ACCOUNT_ID_A2));
            transferOperationRepository.create(
                new Transfer(transferBId, USER_ID_B, Money.of("300.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_B1, ACCOUNT_ID_B2));
        });

        List<Transfer> userATransfers = transactionManager.execute(() -> transferOperationRepository.findByUserId(USER_ID_A));
        List<Transfer> userBTransfers = transactionManager.execute(() -> transferOperationRepository.findByUserId(USER_ID_B));

        assertEquals(2, userATransfers.size());
        assertEquals(1, userBTransfers.size());

        Optional<Transfer> transferBViaUserA = transactionManager.execute(
            () -> transferOperationRepository.findByIdAndUserId(transferBId, USER_ID_A));
        assertFalse(transferBViaUserA.isPresent());
    }

    @Test
    void ordersTransfersByOperationDateThenIdDescending() {
        UUID idEarlierDateHighId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID idLaterDateLowId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        transactionManager.execute(() -> {
            transferOperationRepository.create(
                new Transfer(idEarlierDateHighId, USER_ID_A, Money.of("100.00"), LocalDate.of(2026, 9, 1), ACCOUNT_ID_A1, ACCOUNT_ID_A2));
            transferOperationRepository.create(
                new Transfer(idLaterDateLowId, USER_ID_A, Money.of("200.00"), LocalDate.of(2026, 9, 2), ACCOUNT_ID_A1, ACCOUNT_ID_A2));
        });

        List<Transfer> userATransfers = transactionManager.execute(() -> transferOperationRepository.findByUserId(USER_ID_A));

        assertEquals(2, userATransfers.size());
        assertEquals(idLaterDateLowId, userATransfers.get(0).id());
        assertEquals(idEarlierDateHighId, userATransfers.get(1).id());
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidSourceAccount() {
        UUID invalidSourceAccountId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Transfer transfer = new Transfer(USER_ID_A, Money.of("100.00"), LocalDate.now(), invalidSourceAccountId, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        }));
    }

    @Test
    void violatesForeignKeyConstraintOnInvalidTargetAccount() {
        UUID invalidTargetAccountId = UUID.randomUUID();

        assertThrows(RuntimeException.class, () -> transactionManager.execute(() -> {
            Transfer transfer = new Transfer(USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A1, invalidTargetAccountId);
            transferOperationRepository.create(transfer);
        }));
    }

    @Test
    void deleteByIdRemovesRecord() {
        UUID transferId = UUID.randomUUID();

        transactionManager.execute(() -> {
            Transfer transfer = new Transfer(transferId, USER_ID_A, Money.of("100.00"), LocalDate.now(), ACCOUNT_ID_A1, ACCOUNT_ID_A2);
            transferOperationRepository.create(transfer);
        });

        transactionManager.execute(() -> transferOperationRepository.deleteById(transferId));

        Optional<Transfer> found = transactionManager.execute(() -> transferOperationRepository.findById(transferId));

        assertFalse(found.isPresent());
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        assertThrows(IllegalStateException.class,
            () -> transferOperationRepository.findById(UUID.randomUUID()));
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
