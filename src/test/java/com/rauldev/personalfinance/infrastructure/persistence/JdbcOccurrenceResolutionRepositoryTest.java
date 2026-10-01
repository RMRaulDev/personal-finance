package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
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
import com.rauldev.personalfinance.domain.Expense;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcOccurrenceResolutionRepositoryTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OBLIGATION_ID = UUID.randomUUID();
    private static final UUID OTHER_OBLIGATION_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final Instant RESOLVED_AT = Instant.parse("2026-09-02T10:15:30.123Z");

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcOccurrenceResolutionRepository resolutionRepository;
    private JdbcExpenseOperationRepository expenseRepository;

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        resolutionRepository = new JdbcOccurrenceResolutionRepository(connectionHolder);
        expenseRepository = new JdbcExpenseOperationRepository(connectionHolder);
        JdbcAccountRepository accountRepository = new JdbcAccountRepository(connectionHolder);
        JdbcCategoryRepository categoryRepository = new JdbcCategoryRepository(connectionHolder);
        JdbcObligationRepository obligationRepository = new JdbcObligationRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO users (id) VALUES ('" + USER_ID + "')");
            }
        }

        Recurrence recurrence = new Recurrence(Frequency.MONTHLY, START, null);
        transactionManager.execute(() -> {
            accountRepository.create(new Account(ACCOUNT_ID, USER_ID, "Checking"));
            categoryRepository.create(new Category(CATEGORY_ID, USER_ID, "Rent", CategoryType.EXPENSE));
            obligationRepository.create(new Obligation(OBLIGATION_ID, USER_ID, "Rent", Money.ofCents(10_000),
                ACCOUNT_ID, CATEGORY_ID, recurrence, ObligationStatus.ACTIVE));
            obligationRepository.create(new Obligation(OTHER_OBLIGATION_ID, USER_ID, "Internet", Money.ofCents(5_000),
                ACCOUNT_ID, CATEGORY_ID, recurrence, ObligationStatus.ACTIVE));
        });
    }

    @Test
    void createsAndReadsPaidResolutionWithExpense() {
        UUID expenseId = createExpense();
        OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), OBLIGATION_ID,
            LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId, RESOLVED_AT);

        OccurrenceResolution created = transactionManager.execute(() -> resolutionRepository.create(resolution));
        OccurrenceResolution found = transactionManager.execute(() -> resolutionRepository
            .findByObligationIdAndDueDate(OBLIGATION_ID, LocalDate.of(2026, 9, 1))).orElseThrow();

        assertEquals(resolution, created);
        assertEquals(resolution.id(), found.id());
        assertEquals(OBLIGATION_ID, found.obligationId());
        assertEquals(LocalDate.of(2026, 9, 1), found.dueDate());
        assertEquals(ResolutionStatus.PAID, found.status());
        assertEquals(Optional.of(expenseId), found.expenseId());
        assertEquals(RESOLVED_AT, found.resolvedAt());
    }

    @Test
    void createsAndReadsSkippedResolutionWithoutExpense() {
        OccurrenceResolution resolution = new OccurrenceResolution(UUID.randomUUID(), OBLIGATION_ID,
            LocalDate.of(2026, 9, 1), ResolutionStatus.SKIPPED, null, RESOLVED_AT);

        transactionManager.execute(() -> resolutionRepository.create(resolution));
        OccurrenceResolution found = transactionManager.execute(() -> resolutionRepository
            .findByObligationIdAndDueDate(OBLIGATION_ID, LocalDate.of(2026, 9, 1))).orElseThrow();

        assertEquals(resolution.id(), found.id());
        assertEquals(ResolutionStatus.SKIPPED, found.status());
        assertTrue(found.expenseId().isEmpty());
        assertEquals(RESOLVED_AT, found.resolvedAt());
    }

    @Test
    void findsResolutionsByObligationIdOrderedByDueDateAndScopedToTheObligation() {
        OccurrenceResolution october = skipped(OBLIGATION_ID, LocalDate.of(2026, 10, 1));
        OccurrenceResolution august = skipped(OBLIGATION_ID, LocalDate.of(2026, 8, 1));
        OccurrenceResolution september = skipped(OBLIGATION_ID, LocalDate.of(2026, 9, 1));
        OccurrenceResolution other = skipped(OTHER_OBLIGATION_ID, LocalDate.of(2026, 9, 1));
        transactionManager.execute(() -> {
            resolutionRepository.create(october);
            resolutionRepository.create(august);
            resolutionRepository.create(other);
            resolutionRepository.create(september);
        });

        List<OccurrenceResolution> found = transactionManager.execute(
            () -> resolutionRepository.findByObligationId(OBLIGATION_ID));

        assertEquals(List.of(august.id(), september.id(), october.id()),
            found.stream().map(OccurrenceResolution::id).toList());
    }

    @Test
    void findsNoResolutionsForObligationWithoutResolutions() {
        List<OccurrenceResolution> found = transactionManager.execute(
            () -> resolutionRepository.findByObligationId(OBLIGATION_ID));

        assertTrue(found.isEmpty());
    }

    @Test
    void findByObligationIdAndDueDateMatchesOnlyThatOccurrence() {
        transactionManager.execute(() -> resolutionRepository.create(skipped(OBLIGATION_ID, LocalDate.of(2026, 9, 1))));

        Optional<OccurrenceResolution> otherDate = transactionManager.execute(() -> resolutionRepository
            .findByObligationIdAndDueDate(OBLIGATION_ID, LocalDate.of(2026, 10, 1)));
        Optional<OccurrenceResolution> otherObligation = transactionManager.execute(() -> resolutionRepository
            .findByObligationIdAndDueDate(OTHER_OBLIGATION_ID, LocalDate.of(2026, 9, 1)));

        assertFalse(otherDate.isPresent());
        assertFalse(otherObligation.isPresent());
    }

    @Test
    void findsResolutionByExpenseId() {
        UUID expenseId = createExpense();
        OccurrenceResolution paid = new OccurrenceResolution(UUID.randomUUID(), OBLIGATION_ID,
            LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId, RESOLVED_AT);
        transactionManager.execute(() -> resolutionRepository.create(paid));

        Optional<OccurrenceResolution> found = transactionManager.execute(
            () -> resolutionRepository.findByExpenseId(expenseId));
        Optional<OccurrenceResolution> unknown = transactionManager.execute(
            () -> resolutionRepository.findByExpenseId(UUID.randomUUID()));

        assertEquals(paid.id(), found.orElseThrow().id());
        assertFalse(unknown.isPresent());
    }

    @Test
    void deletesResolutionWithoutAffectingOthers() {
        OccurrenceResolution first = skipped(OBLIGATION_ID, LocalDate.of(2026, 8, 1));
        OccurrenceResolution second = skipped(OBLIGATION_ID, LocalDate.of(2026, 9, 1));
        transactionManager.execute(() -> {
            resolutionRepository.create(first);
            resolutionRepository.create(second);
        });

        transactionManager.execute(() -> resolutionRepository.delete(first.id()));
        List<OccurrenceResolution> remaining = transactionManager.execute(
            () -> resolutionRepository.findByObligationId(OBLIGATION_ID));

        assertEquals(List.of(second.id()), remaining.stream().map(OccurrenceResolution::id).toList());
    }

    @Test
    void deletingMissingResolutionIsANoOp() {
        OccurrenceResolution existing = skipped(OBLIGATION_ID, LocalDate.of(2026, 8, 1));
        transactionManager.execute(() -> resolutionRepository.create(existing));

        transactionManager.execute(() -> resolutionRepository.delete(UUID.randomUUID()));

        assertEquals(1, transactionManager.execute(() -> resolutionRepository.findByObligationId(OBLIGATION_ID)).size());
    }

    @Test
    void deletedPaidResolutionFreesTheExpenseAndTheDueDate() {
        UUID expenseId = createExpense();
        OccurrenceResolution paid = new OccurrenceResolution(UUID.randomUUID(), OBLIGATION_ID,
            LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId, RESOLVED_AT);
        transactionManager.execute(() -> resolutionRepository.create(paid));

        transactionManager.execute(() -> resolutionRepository.delete(paid.id()));

        assertFalse(transactionManager.execute(() -> resolutionRepository.findByExpenseId(expenseId)).isPresent());
        createsAndFinds(skipped(OBLIGATION_ID, LocalDate.of(2026, 9, 1)));
    }

    @Test
    void createWithDuplicateObligationAndDueDateThrowsBusinessRuleViolation() {
        LocalDate dueDate = LocalDate.of(2026, 9, 1);
        transactionManager.execute(() -> resolutionRepository.create(skipped(OBLIGATION_ID, dueDate)));

        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
            () -> transactionManager.execute(() -> resolutionRepository.create(skipped(OBLIGATION_ID, dueDate))));

        assertEquals(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED, ex.code());
        assertEquals(ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void sameDueDateIsAllowedForDifferentObligations() {
        LocalDate dueDate = LocalDate.of(2026, 9, 1);

        transactionManager.execute(() -> {
            resolutionRepository.create(skipped(OBLIGATION_ID, dueDate));
            resolutionRepository.create(skipped(OTHER_OBLIGATION_ID, dueDate));
        });

        assertEquals(1, transactionManager.execute(() -> resolutionRepository.findByObligationId(OBLIGATION_ID)).size());
        assertEquals(1, transactionManager.execute(() -> resolutionRepository.findByObligationId(OTHER_OBLIGATION_ID)).size());
    }

    @Test
    void createWithDuplicateExpenseIdOnDifferentDueDateIsNotTranslatedToBusinessRuleViolation() {
        UUID expenseId = createExpense();
        transactionManager.execute(() -> resolutionRepository.create(new OccurrenceResolution(UUID.randomUUID(),
            OBLIGATION_ID, LocalDate.of(2026, 8, 1), ResolutionStatus.PAID, expenseId, RESOLVED_AT)));

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> resolutionRepository.create(new OccurrenceResolution(
                UUID.randomUUID(), OBLIGATION_ID, LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, expenseId,
                RESOLVED_AT))));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create occurrence resolution", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void createWithUnknownExpenseFailsWithGenericRuntimeException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> transactionManager.execute(() -> resolutionRepository.create(new OccurrenceResolution(
                UUID.randomUUID(), OBLIGATION_ID, LocalDate.of(2026, 9, 1), ResolutionStatus.PAID, UUID.randomUUID(),
                RESOLVED_AT))));

        assertFalse(ex instanceof BusinessRuleViolationException);
        assertEquals("Failed to create occurrence resolution", ex.getMessage());
        assertInstanceOf(SQLiteException.class, ex.getCause());
    }

    @Test
    void databaseRejectsPaidResolutionWithoutExpense() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(rawInsert(id, OBLIGATION_ID, "PAID", null)));

        assertEquals(0L, countRows(id));
    }

    @Test
    void databaseRejectsSkippedResolutionWithExpense() throws SQLException {
        UUID expenseId = createExpense();
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(rawInsert(id, OBLIGATION_ID, "SKIPPED", expenseId)));

        assertEquals(0L, countRows(id));
    }

    @Test
    void databaseRejectsResolutionWithUnknownStatus() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(rawInsert(id, OBLIGATION_ID, "BOGUS", null)));

        assertEquals(0L, countRows(id));
    }

    @Test
    void databaseRejectsResolutionWithUnknownObligation() throws SQLException {
        UUID id = UUID.randomUUID();

        assertThrows(SQLException.class,
            () -> executeOnNormalConnection(rawInsert(id, UUID.randomUUID(), "SKIPPED", null)));

        assertEquals(0L, countRows(id));
    }

    @Test
    void databaseAcceptsPaidResolutionWithExpenseOnNormalConnection() throws SQLException {
        UUID expenseId = createExpense();
        UUID id = UUID.randomUUID();

        executeOnNormalConnection(rawInsert(id, OBLIGATION_ID, "PAID", expenseId));

        assertEquals(1L, countRows(id));
    }

    @Test
    void requiresActiveTransactionForRepositoryOperations() {
        OccurrenceResolution resolution = skipped(OBLIGATION_ID, LocalDate.of(2026, 9, 1));

        assertThrows(IllegalStateException.class, () -> resolutionRepository.create(resolution));
        assertThrows(IllegalStateException.class, () -> resolutionRepository.findByObligationId(OBLIGATION_ID));
        assertThrows(IllegalStateException.class,
            () -> resolutionRepository.findByObligationIdAndDueDate(OBLIGATION_ID, LocalDate.of(2026, 9, 1)));
        assertThrows(IllegalStateException.class, () -> resolutionRepository.findByExpenseId(UUID.randomUUID()));
        assertThrows(IllegalStateException.class, () -> resolutionRepository.delete(resolution.id()));
    }

    @Test
    void failsWithCorruptedPersistedDataWhenIdIsNotAUuid() throws SQLException {
        insertRawRow("not-a-uuid", "2026-09-01", "SKIPPED", null, RESOLVED_AT.toString());

        assertCorrupted("not-a-uuid", IllegalArgumentException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenStatusIsUnknown() throws SQLException {
        UUID id = UUID.randomUUID();
        // The CHECK on status is bypassed on purpose to simulate a corrupted row.
        insertRawRow(id.toString(), "2026-09-01", "BOGUS", null, RESOLVED_AT.toString());

        assertCorrupted(id.toString(), IllegalArgumentException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenResolvedAtIsInvalid() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id.toString(), "2026-09-01", "SKIPPED", null, "not-an-instant");

        assertCorrupted(id.toString(), DateTimeParseException.class);
    }

    @Test
    void failsWithCorruptedPersistedDataWhenDueDateIsInvalid() throws SQLException {
        UUID id = UUID.randomUUID();
        insertRawRow(id.toString(), "not-a-date", "SKIPPED", null, RESOLVED_AT.toString());

        assertCorrupted(id.toString(), DateTimeParseException.class);
    }

    private void assertCorrupted(String rowId, Class<? extends Throwable> causeType) {
        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> transactionManager.execute(() -> resolutionRepository.findByObligationId(OBLIGATION_ID)));

        assertTrue(ex.getMessage().contains("occurrence_resolutions"));
        assertTrue(ex.getMessage().contains(rowId));
        assertInstanceOf(causeType, ex.getCause());
    }

    private void createsAndFinds(OccurrenceResolution resolution) {
        transactionManager.execute(() -> resolutionRepository.create(resolution));

        assertTrue(transactionManager.execute(() -> resolutionRepository
            .findByObligationIdAndDueDate(resolution.obligationId(), resolution.dueDate())).isPresent());
    }

    private OccurrenceResolution skipped(UUID obligationId, LocalDate dueDate) {
        return new OccurrenceResolution(UUID.randomUUID(), obligationId, dueDate, ResolutionStatus.SKIPPED, null,
            RESOLVED_AT);
    }

    private UUID createExpense() {
        UUID expenseId = UUID.randomUUID();
        transactionManager.execute(() -> expenseRepository.create(new Expense(expenseId, USER_ID,
            Money.ofCents(10_000), LocalDate.of(2026, 9, 1), ACCOUNT_ID, CATEGORY_ID)));
        return expenseId;
    }

    private String rawInsert(UUID id, UUID obligationId, String status, UUID expenseId) {
        String expense = expenseId == null ? "NULL" : "'" + expenseId + "'";
        return "INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, resolved_at) "
            + "VALUES ('" + id + "', '" + obligationId + "', '2026-09-01', '" + status + "', " + expense + ", '"
            + RESOLVED_AT + "')";
    }

    private void executeOnNormalConnection(String sql) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private long countRows(UUID id) throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM occurrence_resolutions WHERE id = '" + id + "'")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private void insertRawRow(String id, String dueDate, String status, String expenseId, String resolvedAt)
            throws SQLException {
        String expense = expenseId == null ? "NULL" : "'" + expenseId + "'";
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA ignore_check_constraints = ON");
            stmt.execute("INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, "
                + "resolved_at) VALUES ('" + id + "', '" + OBLIGATION_ID + "', '" + dueDate + "', '" + status + "', "
                + expense + ", '" + resolvedAt + "')");
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
}
