package com.rauldev.personalfinance.infrastructure.persistence;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

class JdbcObligationQueryAdapterTest {

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcObligationQueryAdapter adapter;

    private static final UUID USER_A_ID = UUID.randomUUID();
    private static final UUID USER_B_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_A_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_B_ID = UUID.randomUUID();
    private static final UUID CATEGORY_A_ID = UUID.randomUUID();
    private static final UUID CATEGORY_B_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        Path dbPath = tempDir.resolve("test-finance.db");
        String jdbcUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        connectionProvider = new SQLiteConnectionProvider(jdbcUrl);
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        adapter = new JdbcObligationQueryAdapter(connectionProvider, connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            exec(conn, "INSERT INTO users (id) VALUES ('" + USER_A_ID + "')");
            exec(conn, "INSERT INTO users (id) VALUES ('" + USER_B_ID + "')");
            exec(conn, "INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + ACCOUNT_A_ID
                + "', '" + USER_A_ID + "', 'Wallet', 0, 'ACTIVE')");
            exec(conn, "INSERT INTO accounts (id, user_id, name, balance, status) VALUES ('" + ACCOUNT_B_ID
                + "', '" + USER_B_ID + "', 'Theirs', 0, 'ACTIVE')");
            exec(conn, "INSERT INTO categories (id, user_id, name, type, status) VALUES ('" + CATEGORY_A_ID
                + "', '" + USER_A_ID + "', 'Rent', 'EXPENSE', 'ACTIVE')");
            exec(conn, "INSERT INTO categories (id, user_id, name, type, status) VALUES ('" + CATEGORY_B_ID
                + "', '" + USER_B_ID + "', 'Their rent', 'EXPENSE', 'ACTIVE')");
        }
    }

    // ---------- findByUserId ----------

    @Test
    void findByUserId_returnsActiveAndArchivedObligations() {
        insertObligation(UUID.randomUUID(), USER_A_ID, "Active one", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");
        insertObligation(UUID.randomUUID(), USER_A_ID, "Archived one", 100, "MONTHLY", "2026-10-20", null,
            "ARCHIVED");

        List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

        assertEquals(List.of(ObligationStatus.ACTIVE, ObligationStatus.ARCHIVED),
            obligations.stream().map(ObligationDetails::status).toList());
    }

    @Test
    void findByUserId_ordersByNameWithBinaryCollationUppercaseBeforeLowercase() {
        insertObligation(UUID.randomUUID(), USER_A_ID, "alpha", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");
        insertObligation(UUID.randomUUID(), USER_A_ID, "Zeta", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");

        List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

        assertEquals(List.of("Zeta", "alpha"), obligations.stream().map(ObligationDetails::name).toList());
    }

    @Test
    void findByUserId_excludesOtherUsersObligations() {
        UUID ownId = UUID.randomUUID();
        insertObligation(ownId, USER_A_ID, "Mine", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");
        insertObligation(UUID.randomUUID(), USER_B_ID, "Theirs", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_B_ID, CATEGORY_B_ID);

        List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

        assertEquals(1, obligations.size());
        assertEquals(ownId, obligations.get(0).id());
    }

    @Test
    void findByUserId_returnsEmptyListWhenUserHasNoObligations() {
        insertObligation(UUID.randomUUID(), USER_B_ID, "Theirs", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_B_ID, CATEGORY_B_ID);

        assertEquals(List.of(), adapter.findByUserId(USER_A_ID));
    }

    @Test
    void findByUserId_mapsEveryFieldWithEndDate() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 2550, "BIWEEKLY", "2026-10-01", "2026-12-31", "ARCHIVED");

        List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

        assertEquals(List.of(expected(id, "Rent", 2550, Frequency.BIWEEKLY, LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 12, 31), ObligationStatus.ARCHIVED)), obligations);
    }

    @Test
    void findByUserId_mapsAnOpenEndedCalendarToEmptyEndDate() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 100, "ONCE", "2026-10-01", null, "ACTIVE");

        List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

        assertEquals(Optional.empty(), obligations.get(0).recurrence().endDate());
        assertEquals(Frequency.ONCE, obligations.get(0).recurrence().frequency());
    }

    @Test
    void findByUserId_worksOutsideAndInsideActiveTransaction() {
        insertObligation(UUID.randomUUID(), USER_A_ID, "Rent", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");

        List<ObligationDetails> outside = adapter.findByUserId(USER_A_ID);
        List<ObligationDetails> inside = transactionManager.execute(() -> adapter.findByUserId(USER_A_ID));

        assertEquals(1, outside.size());
        assertEquals(outside, inside);
    }

    @Test
    void findByUserId_insideTransactionReusesBoundConnectionWithoutClosingIt() {
        UUID uncommitted = UUID.randomUUID();

        transactionManager.execute(() -> {
            Connection bound = connectionHolder.get();
            try (Statement stmt = bound.createStatement()) {
                stmt.executeUpdate("INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, "
                    + "frequency, start_date, end_date, status) VALUES ('" + uncommitted + "', '" + USER_A_ID
                    + "', 'Pending', 100, '" + ACCOUNT_A_ID + "', '" + CATEGORY_A_ID
                    + "', 'MONTHLY', '2026-10-20', NULL, 'ACTIVE')");

                List<ObligationDetails> obligations = adapter.findByUserId(USER_A_ID);

                assertEquals(1, obligations.size());
                assertEquals(uncommitted, obligations.get(0).id());
                assertFalse(bound.isClosed());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void findByUserId_rejectsNullUserId() {
        assertThrows(NullPointerException.class, () -> adapter.findByUserId(null));
    }

    // ---------- findByIdAndUserId ----------

    @Test
    void findByIdAndUserId_findsTheOwnObligationWithEveryField() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 2500, "MONTHLY", "2026-10-20", null, "ACTIVE");

        Optional<ObligationDetails> found = adapter.findByIdAndUserId(id, USER_A_ID);

        assertEquals(Optional.of(expected(id, "Rent", 2500, Frequency.MONTHLY, LocalDate.of(2026, 10, 20), null,
            ObligationStatus.ACTIVE)), found);
    }

    @Test
    void findByIdAndUserId_findsAnArchivedObligationWithEndDate() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 2500, "YEARLY", "2026-10-20", "2027-10-20", "ARCHIVED");

        Optional<ObligationDetails> found = adapter.findByIdAndUserId(id, USER_A_ID);

        assertEquals(Optional.of(expected(id, "Rent", 2500, Frequency.YEARLY, LocalDate.of(2026, 10, 20),
            LocalDate.of(2027, 10, 20), ObligationStatus.ARCHIVED)), found);
    }

    @Test
    void findByIdAndUserId_doesNotFindAnotherUsersObligation() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_B_ID, "Theirs", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_B_ID, CATEGORY_B_ID);

        assertEquals(Optional.empty(), adapter.findByIdAndUserId(id, USER_A_ID));
    }

    @Test
    void findByIdAndUserId_returnsEmptyWhenIdIsUnknown() {
        assertEquals(Optional.empty(), adapter.findByIdAndUserId(UUID.randomUUID(), USER_A_ID));
    }

    @Test
    void findByIdAndUserId_worksOutsideAndInsideActiveTransaction() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");

        Optional<ObligationDetails> outside = adapter.findByIdAndUserId(id, USER_A_ID);
        Optional<ObligationDetails> inside =
            transactionManager.execute(() -> adapter.findByIdAndUserId(id, USER_A_ID));

        assertTrue(outside.isPresent());
        assertEquals(outside, inside);
    }

    @Test
    void findByIdAndUserId_insideTransactionDoesNotCloseTheBoundConnection() {
        UUID id = UUID.randomUUID();
        insertObligation(id, USER_A_ID, "Rent", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");

        transactionManager.execute(() -> {
            Connection bound = connectionHolder.get();
            try {
                assertTrue(adapter.findByIdAndUserId(id, USER_A_ID).isPresent());
                assertFalse(bound.isClosed());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void findByIdAndUserId_rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> adapter.findByIdAndUserId(null, USER_A_ID));
        assertThrows(NullPointerException.class, () -> adapter.findByIdAndUserId(UUID.randomUUID(), null));
    }

    // ---------- corrupt rows ----------

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenFrequencyIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "BOGUS", "2026-10-20", null, "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, IllegalArgumentException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenStatusIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "2026-10-20", null, "BOGUS");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, IllegalArgumentException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenStartDateIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "not-a-date", null, "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, DateTimeParseException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenEndDateIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "2026-10-20", "zzz", "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, DateTimeParseException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenNameIsBlank() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "  ", 100, "MONTHLY", "2026-10-20", null, "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, IllegalArgumentException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenAccountBelongsToAnotherUser() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_B_ID, CATEGORY_A_ID);

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, NullPointerException.class);
    }

    @Test
    void findByUserId_failsWithCorruptedPersistedDataWhenCategoryBelongsToAnotherUser() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_A_ID, CATEGORY_B_ID);

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByUserId(USER_A_ID));

        assertCorrupted(ex, rowId, NullPointerException.class);
    }

    @Test
    void findByIdAndUserId_failsWithCorruptedPersistedDataWhenFrequencyIsInvalid() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "BOGUS", "2026-10-20", null, "ACTIVE");

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByIdAndUserId(rowId, USER_A_ID));

        assertCorrupted(ex, rowId, IllegalArgumentException.class);
    }

    @Test
    void findByIdAndUserId_failsWithCorruptedPersistedDataWhenAccountBelongsToAnotherUser() {
        UUID rowId = UUID.randomUUID();
        insertObligation(rowId, USER_A_ID, "Broken", 100, "MONTHLY", "2026-10-20", null, "ACTIVE",
            ACCOUNT_B_ID, CATEGORY_A_ID);

        CorruptedPersistedDataException ex = assertThrows(CorruptedPersistedDataException.class,
            () -> adapter.findByIdAndUserId(rowId, USER_A_ID));

        assertCorrupted(ex, rowId, NullPointerException.class);
    }

    // ---------- constructor ----------

    @Test
    void constructorRejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new JdbcObligationQueryAdapter(null, connectionHolder));
        assertThrows(NullPointerException.class, () -> new JdbcObligationQueryAdapter(connectionProvider, null));
    }

    // ---------- helpers ----------

    private static void assertCorrupted(CorruptedPersistedDataException ex, UUID rowId,
                                        Class<? extends Throwable> cause) {
        assertTrue(ex.getMessage().contains("obligations"));
        assertTrue(ex.getMessage().contains(rowId.toString()));
        assertInstanceOf(cause, ex.getCause());
    }

    private static ObligationDetails expected(UUID id, String name, long cents, Frequency frequency,
                                              LocalDate start, LocalDate end, ObligationStatus status) {
        return new ObligationDetails(id, name, Money.ofCents(cents), new AccountSummary(ACCOUNT_A_ID, "Wallet"),
            new CategorySummary(CATEGORY_A_ID, "Rent"), new Recurrence(frequency, start, end), status);
    }

    private void insertObligation(UUID id, UUID userId, String name, long amount, String frequency, String start,
                                  String end, String status) {
        insertObligation(id, userId, name, amount, frequency, start, end, status, ACCOUNT_A_ID, CATEGORY_A_ID);
    }

    private void insertObligation(UUID id, UUID userId, String name, long amount, String frequency, String start,
                                  String end, String status, UUID accountId, UUID categoryId) {
        String sql = "INSERT INTO obligations (id, user_id, name, amount, account_id, category_id, frequency, "
            + "start_date, end_date, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id.toString());
            stmt.setString(2, userId.toString());
            stmt.setString(3, name);
            stmt.setLong(4, amount);
            stmt.setString(5, accountId.toString());
            stmt.setString(6, categoryId.toString());
            stmt.setString(7, frequency);
            stmt.setString(8, start);
            stmt.setString(9, end);
            stmt.setString(10, status);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void exec(Connection connection, String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
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
                    exec(connection, trimmed);
                }
            }
        }
    }
}
