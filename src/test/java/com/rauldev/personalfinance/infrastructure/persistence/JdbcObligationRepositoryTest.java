package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.sqlite.SQLiteException;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcObligationRepositoryTest {

    private static final UUID USER_ID_A = UUID.randomUUID();
    private static final UUID USER_ID_B = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_A2 = UUID.randomUUID();
    private static final UUID ACCOUNT_ID_B = UUID.randomUUID();
    private static final UUID CATEGORY_ID_A = UUID.randomUUID();
    private static final UUID CATEGORY_ID_A2 = UUID.randomUUID();
    private static final UUID CATEGORY_ID_B = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 20);

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcObligationRepository obligationRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        obligationRepository = new JdbcObligationRepository(connectionHolder);
        JdbcAccountRepository accountRepository = new JdbcAccountRepository(connectionHolder);
        JdbcCategoryRepository categoryRepository = new JdbcCategoryRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            seedUser(conn, USER_ID_A);
            seedUser(conn, USER_ID_B);
        }

        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID_A, USER_ID_A, "Account A"));
            accountRepository.create(new Account(ACCOUNT_ID_A2, USER_ID_A, "Account A2"));
            accountRepository.create(new Account(ACCOUNT_ID_B, USER_ID_B, "Account B"));
            categoryRepository.create(new Category(CATEGORY_ID_A, USER_ID_A, "Rent", CategoryType.EXPENSE));
            categoryRepository.create(new Category(CATEGORY_ID_A2, USER_ID_A, "Utilities", CategoryType.EXPENSE));
            categoryRepository.create(new Category(CATEGORY_ID_B, USER_ID_B, "Rent", CategoryType.EXPENSE));
        });
    }

    @Test
    void createsAndFindsObligationRoundTrippingEveryField() {
        UUID id = UUID.randomUUID();
        LocalDate end = LocalDate.of(2027, 8, 20);
        Obligation obligation = new Obligation(id, USER_ID_A, "Rent", Money.ofCents(1_234_567), ACCOUNT_ID_A,
            CATEGORY_ID_A, new Recurrence(Frequency.MONTHLY, START, end), ObligationStatus.ARCHIVED);

        Obligation created = transactionManager.execute(() -> obligationRepository.create(obligation));
        Optional<Obligation> found = transactionManager.execute(() -> obligationRepository.findByIdAndUserId(id, USER_ID_A));

        assertEquals(obligation, created);
        assertTrue(found.isPresent());
        assertEquals(id, found.get().id());
        assertEquals(USER_ID_A, found.get().userId());
        assertEquals("Rent", found.get().name());
        assertEquals(Money.ofCents(1_234_567), found.get().amount());
        assertEquals(ACCOUNT_ID_A, found.get().accountId());
        assertEquals(CATEGORY_ID_A, found.get().categoryId());
        assertEquals(new Recurrence(Frequency.MONTHLY, START, end), found.get().recurrence());
        assertEquals(ObligationStatus.ARCHIVED, found.get().status());
    }

    @Test
    void storesAmountInCents() throws SQLException {
        UUID id = UUID.randomUUID();

        transactionManager.execute(() -> obligationRepository.create(
            obligation(id, USER_ID_A, "Rent", 45_075, new Recurrence(Frequency.ONCE, START, null),
                ObligationStatus.ACTIVE)));

        assertEquals(45_075L, queryLong("SELECT amount FROM obligations WHERE id = '" + id + "'"));
    }

    @Test
    void roundTripsEveryFrequencyWithoutEndDate() {
        for (Frequency frequency : Frequency.values()) {
            UUID id = UUID.randomUUID();
            Recurrence recurrence = new Recurrence(frequency, START, null);

            transactionManager.execute(() -> obligationRepository.create(
                obligation(id, USER_ID_A, "Obligation " + frequency, 10_000, recurrence, ObligationStatus.ACTIVE)));
            Obligation found = transactionManager.execute(
                () -> obligationRepository.findByIdAndUserId(id, USER_ID_A)).orElseThrow();

            assertEquals(frequency, found.recurrence().frequency());
            assertEquals(START, found.recurrence().startDate());
            assertTrue(found.recurrence().endDate().isEmpty());
            assertEquals(ObligationStatus.ACTIVE, found.status());
        }
    }

    @Test
    void roundTripsEveryFrequencyWithEndDate() {
        LocalDate end = LocalDate.of(2028, 1, 31);
        for (Frequency frequency : Frequency.values()) {
            UUID id = UUID.randomUUID();

            transactionManager.execute(() -> obligationRepository.create(
                obligation(id, USER_ID_A, "Obligation " + frequency, 10_000, new Recurrence(frequency, START, end),
                    ObligationStatus.ACTIVE)));
            Obligation found = transactionManager.execute(
                () -> obligationRepository.findByIdAndUserId(id, USER_ID_A)).orElseThrow();

            assertEquals(frequency, found.recurrence().frequency());
            assertEquals(Optional.of(end), found.recurrence().endDate());
        }
    }

    @Test
    void doesNotFindObligationWhenUserIdDoesNotMatch() {
        UUID id = UUID.randomUUID();
        transactionManager.execute(() -> obligationRepository.create(
            obligation(id, USER_ID_A, "Rent", 10_000, new Recurrence(Frequency.MONTHLY, START, null),
                ObligationStatus.ACTIVE)));

        Optional<Obligation> found = transactionManager.execute(
            () -> obligationRepository.findByIdAndUserId(id, USER_ID_B));

        assertFalse(found.isPresent());
    }

    @Test
    void doesNotFindUnknownObligation() {
        Optional<Obligation> found = transactionManager.execute(
            () -> obligationRepository.findByIdAndUserId(UUID.randomUUID(), USER_ID_A));

        assertFalse(found.isPresent());
    }

    @Test
    void checksExistenceByUserIdAndName() {
        transactionManager.execute(() -> obligationRepository.create(
            obligation(UUID.randomUUID(), USER_ID_A, "Rent", 10_000,
                new Recurrence(Frequency.MONTHLY, START, null), ObligationStatus.ACTIVE)));

        boolean existsForUserA = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndName(USER_ID_A, "Rent"));
        boolean unknownNameForUserA = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndName(USER_ID_A, "Unknown"));
        boolean existsForUserB = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndName(USER_ID_B, "Rent"));

        assertTrue(existsForUserA);
        assertFalse(unknownNameForUserA);
        assertFalse(existsForUserB);
    }

    @Test
    void checksExistenceByUserIdAndNameExcludingId() {
        UUID rentId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> {
            obligationRepository.create(obligation(rentId, USER_ID_A, "Rent", 10_000, recurrence, ObligationStatus.ACTIVE));
            obligationRepository.create(obligation(otherId, USER_ID_A, "Internet", 10_000, recurrence, ObligationStatus.ACTIVE));
        });

        boolean ownName = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndNameAndIdNot(USER_ID_A, "Rent", rentId));
        boolean nameOfAnother = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndNameAndIdNot(USER_ID_A, "Rent", otherId));
        boolean nameOfAnotherUser = transactionManager.execute(
            () -> obligationRepository.existsByUserIdAndNameAndIdNot(USER_ID_B, "Rent", otherId));

        assertFalse(ownName);
        assertTrue(nameOfAnother);
        assertFalse(nameOfAnotherUser);
    }

    @Test
    void updatesEveryMutableField() {
        UUID id = UUID.randomUUID();
        LocalDate end = LocalDate.of(2027, 8, 20);
        transactionManager.execute(() -> obligationRepository.create(
            obligation(id, USER_ID_A, "Rent", 10_000, new Recurrence(Frequency.MONTHLY, START, end),
                ObligationStatus.ACTIVE)));
        Recurrence newRecurrence = new Recurrence(Frequency.WEEKLY, LocalDate.of(2026, 9, 1), null);
        Obligation updatedObligation = new Obligation(id, USER_ID_A, "Rent v2", Money.ofCents(99_999), ACCOUNT_ID_A2,
            CATEGORY_ID_A2, newRecurrence, ObligationStatus.ARCHIVED);

        transactionManager.execute(() -> obligationRepository.update(updatedObligation));
        Obligation found = transactionManager.execute(
            () -> obligationRepository.findByIdAndUserId(id, USER_ID_A)).orElseThrow();

        assertEquals(id, found.id());
        assertEquals(USER_ID_A, found.userId());
        assertEquals("Rent v2", found.name());
        assertEquals(Money.ofCents(99_999), found.amount());
        assertEquals(ACCOUNT_ID_A2, found.accountId());
        assertEquals(CATEGORY_ID_A2, found.categoryId());
        assertEquals(newRecurrence, found.recurrence());
        assertTrue(found.recurrence().endDate().isEmpty());
        assertEquals(ObligationStatus.ARCHIVED, found.status());
    }

    @Test
    void updateDoesNotAffectOtherObligations() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> {
            obligationRepository.create(obligation(id, USER_ID_A, "Rent", 10_000, recurrence, ObligationStatus.ACTIVE));
            obligationRepository.create(obligation(otherId, USER_ID_A, "Internet", 20_000, recurrence, ObligationStatus.ACTIVE));
        });

        transactionManager.execute(() -> obligationRepository.update(
            obligation(id, USER_ID_A, "Rent v2", 30_000, recurrence, ObligationStatus.ARCHIVED)));
        Obligation other = transactionManager.execute(
            () -> obligationRepository.findByIdAndUserId(otherId, USER_ID_A)).orElseThrow();

        assertEquals("Internet", other.name());
        assertEquals(Money.ofCents(20_000), other.amount());
        assertEquals(ObligationStatus.ACTIVE, other.status());
    }

    @Test
    void createWithDuplicateUserIdAndNameThrowsBusinessRuleViolation() {
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> obligationRepository.create(
            obligation(UUID.randomUUID(), USER_ID_A, "Rent", 10_000, recurrence, ObligationStatus.ACTIVE)));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
            () -> transactionManager.execute(() -> obligationRepository.create(
                obligation(UUID.randomUUID(), USER_ID_A, "Rent", 20_000, recurrence, ObligationStatus.ACTIVE))));

        assertEquals(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS, ex.code());
        assertEquals(ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void updateRenamingToAnotherRowsNameThrowsBusinessRuleViolation() {
        UUID firstId = UUID.randomUUID();
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> {
            obligationRepository.create(obligation(firstId, USER_ID_A, "First", 10_000, recurrence, ObligationStatus.ACTIVE));
            obligationRepository.create(obligation(UUID.randomUUID(), USER_ID_A, "Second", 10_000, recurrence, ObligationStatus.ACTIVE));
        });

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
            () -> transactionManager.execute(() -> obligationRepository.update(
                obligation(firstId, USER_ID_A, "Second", 10_000, recurrence, ObligationStatus.ACTIVE))));

        assertEquals(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS, ex.code());
        assertEquals(ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
        assertEquals("First", transactionManager.execute(
            () -> obligationRepository.findByIdAndUserId(firstId, USER_ID_A)).orElseThrow().name());
    }

    @Test
    void createsSameNameForDifferentUsers() {
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);

        transactionManager.execute(() -> {
            obligationRepository.create(obligation(idA, USER_ID_A, "Shared", 10_000, recurrence, ObligationStatus.ACTIVE));
            obligationRepository.create(new Obligation(idB, USER_ID_B, "Shared", Money.ofCents(10_000), ACCOUNT_ID_B,
                CATEGORY_ID_B, recurrence, ObligationStatus.ACTIVE));
        });

        assertTrue(transactionManager.execute(() -> obligationRepository.findByIdAndUserId(idA, USER_ID_A)).isPresent());
        assertTrue(transactionManager.execute(() -> obligationRepository.findByIdAndUserId(idB, USER_ID_B)).isPresent());
    }

    @Test
    void createWithDuplicateIdFailsWithGenericRuntimeException() {
        UUID id = UUID.randomUUID();
        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> obligationRepository.create(
            obligation(id, USER_ID_A, "One", 10_000, recurrence, ObligationStatus.ACTIVE)));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> obligationRepository.create(
                obligation(id, USER_ID_A, "Two", 10_000, recurrence, ObligationStatus.ACTIVE))));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create obligation", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void createWithUnknownAccountFailsWithGenericRuntimeException() {
        Obligation orphan = new Obligation(UUID.randomUUID(), USER_ID_A, "Orphan", Money.ofCents(10_000),
            UUID.randomUUID(), CATEGORY_ID_A, new Recurrence(Frequency.ONCE, START, null), ObligationStatus.ACTIVE);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> obligationRepository.create(orphan)));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create obligation", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        UUID id = UUID.randomUUID();
        Obligation obligation = obligation(id, USER_ID_A, "Rent", 10_000,
            new Recurrence(Frequency.MONTHLY, START, null), ObligationStatus.ACTIVE);

        assertThrows(IllegalStateException.class, () -> obligationRepository.create(obligation));
        assertThrows(IllegalStateException.class, () -> obligationRepository.findByIdAndUserId(id, USER_ID_A));
        assertThrows(IllegalStateException.class, () -> obligationRepository.existsByUserIdAndName(USER_ID_A, "Rent"));
        assertThrows(IllegalStateException.class,
            () -> obligationRepository.existsByUserIdAndNameAndIdNot(USER_ID_A, "Rent", id));
        assertThrows(IllegalStateException.class, () -> obligationRepository.update(obligation));
    }

    @Test
    void failsWithCorruptedPersistedDataWhenAccountIdIsNotAUuid() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id, "not-a-uuid", 10_000, "MONTHLY", "2026-08-20", null, "ACTIVE");

        assertCorrupted(id, IllegalArgumentException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenFrequencyIsUnknown() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id, ACCOUNT_ID_A.toString(), 10_000, "DAILY", "2026-08-20", null, "ACTIVE");

        assertCorrupted(id, IllegalArgumentException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenStatusIsUnknown() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id, ACCOUNT_ID_A.toString(), 10_000, "MONTHLY", "2026-08-20", null, "BOGUS");

        assertCorrupted(id, IllegalArgumentException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenStartDateIsInvalid() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id, ACCOUNT_ID_A.toString(), 10_000, "MONTHLY", "not-a-date", null, "ACTIVE");

        assertCorrupted(id, DateTimeParseException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenEndDateIsInvalid() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id, ACCOUNT_ID_A.toString(), 10_000, "MONTHLY", "2026-08-20", "not-a-date", "ACTIVE");

        assertCorrupted(id, DateTimeParseException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenAmountIsZero() throws SQLException {
        UUID id = UUID.randomUUID();
        // The CHECK (amount > 0) constraint is bypassed on purpose to simulate a corrupted row.
        insertRawRow(id, ACCOUNT_ID_A.toString(), 0, "MONTHLY", "2026-08-20", null, "ACTIVE");

        assertCorrupted(id, IllegalArgumentException.class);
    }

    @Test
    void databaseRejectsObligationWithZeroAmount() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(validInsert(id, 0, "2026-08-20", null)));

        assertEquals(0L, queryLong("SELECT COUNT(*) FROM obligations WHERE id = '" + id + "'"));
    }

    @Test
    void databaseRejectsObligationWithNegativeAmount() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(validInsert(id, -1, "2026-08-20", null)));

        assertEquals(0L, queryLong("SELECT COUNT(*) FROM obligations WHERE id = '" + id + "'"));
    }

    @Test
    void databaseRejectsObligationWithEndDateBeforeStartDate() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(validInsert(id, 10_000, "2026-08-20", "2026-08-19")));

        assertEquals(0L, queryLong("SELECT COUNT(*) FROM obligations WHERE id = '" + id + "'"));
    }

    @Test
    void databaseAcceptsObligationWithEndDateEqualToStartDate() throws SQLException {
        UUID id = UUID.randomUUID();

        executeOnNormalConnection(validInsert(id, 1, "2026-08-20", "2026-08-20"));

        assertEquals(1L, queryLong("SELECT COUNT(*) FROM obligations WHERE id = '" + id + "'"));
    }

    private String validInsert(UUID id, long amount, String startDate, String endDate) {
        String end = endDate == null ? "NULL" : "'" + endDate + "'";
        return "INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, "
            + "start_date, end_date, status) VALUES ('" + id + "', '" + USER_ID_A + "', 'Raw', " + amount
            + ", '" + ACCOUNT_ID_A + "', '" + CATEGORY_ID_A + "', 'MONTHLY', '" + startDate + "', " + end
            + ", 'ACTIVE')";
    }

    private void executeOnNormalConnection(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void assertCorrupted(UUID rowId,Class<? extends Throwable> causeType) {
        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> transactionManager.execute(() -> obligationRepository.findByIdAndUserId(rowId, USER_ID_A)));

        assertTrue(ex.getMessage().contains("obligations"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(causeType, ex.getCause());
    }

    private Obligation obligation(UUID id, UUID userId, String name, long cents, Recurrence recurrence,
                                  ObligationStatus status) {
        return new Obligation(id, userId, name, Money.ofCents(cents), ACCOUNT_ID_A, CATEGORY_ID_A, recurrence, status);
    }

    private void insertRawRow(UUID id, String accountId, long amount, String frequency, String startDate,
                              String endDate, String status) throws SQLException {
        String end = endDate == null ? "NULL" : "'" + endDate + "'";
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            stmt.execute("PRAGMA ignore_check_constraints = ON");
            stmt.execute("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, "
                + "start_date, end_date, status) VALUES ('" + id + "', '" + USER_ID_A + "', 'Broken', " + amount
                + ", '" + accountId + "', '" + CATEGORY_ID_A + "', '" + frequency + "', '" + startDate + "', " + end
                + ", '" + status + "')");
        }
    }

    private long queryLong(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
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
