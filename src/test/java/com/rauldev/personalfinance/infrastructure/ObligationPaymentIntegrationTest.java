package com.rauldev.personalfinance.infrastructure;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.usecase.CancelOperation;
import com.rauldev.personalfinance.application.usecase.CancelOperationCommand;
import com.rauldev.personalfinance.application.usecase.ExpenseRegistration;
import com.rauldev.personalfinance.application.usecase.PayOccurrence;
import com.rauldev.personalfinance.application.usecase.PayOccurrenceCommand;
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
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcAccountRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcCategoryRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcExpenseOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcIncomeOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcObligationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcOccurrenceResolutionRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcReversalRepository;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

/**
 * Wires the obligation payment use cases ({@code PayOccurrence}, {@code CancelOperation}) to the real JDBC adapters
 * and a real SQLite database, to verify atomicity (rollback of the expense, the balance change, and the resolution).
 */
class ObligationPaymentIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 20);
    private static final Instant NOW = Instant.parse("2026-08-20T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    /** Weekly (Thursdays) from 2026-08-06. */
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 8, 13);

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OBLIGATION_ID = UUID.randomUUID();

    @TempDir
    Path tempDir;

    private SQLiteConnectionProvider connectionProvider;
    private TransactionConnectionHolder connectionHolder;
    private JdbcTransactionManager transactionManager;
    private JdbcAccountRepository accountRepository;
    private JdbcCategoryRepository categoryRepository;
    private JdbcObligationRepository obligationRepository;
    private JdbcExpenseOperationRepository expenseRepository;
    private JdbcIncomeOperationRepository incomeRepository;
    private JdbcReversalRepository reversalRepository;
    private JdbcOccurrenceResolutionRepository resolutionRepository;

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = new SQLiteConnectionProvider("jdbc:sqlite:" + tempDir.resolve("test-finance.db").toAbsolutePath());
        connectionHolder = new TransactionConnectionHolder();
        transactionManager = new JdbcTransactionManager(connectionProvider, connectionHolder);
        accountRepository = new JdbcAccountRepository(connectionHolder);
        categoryRepository = new JdbcCategoryRepository(connectionHolder);
        obligationRepository = new JdbcObligationRepository(connectionHolder);
        expenseRepository = new JdbcExpenseOperationRepository(connectionHolder);
        incomeRepository = new JdbcIncomeOperationRepository(connectionHolder);
        reversalRepository = new JdbcReversalRepository(connectionHolder);
        resolutionRepository = new JdbcOccurrenceResolutionRepository(connectionHolder);

        try (Connection conn = connectionProvider.getConnection()) {
            initializeSchema(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO users (id) VALUES ('" + USER_ID + "')");
            }
        }

        Account account = new Account(ACCOUNT_ID, USER_ID, "Checking");
        account.credit(Money.ofCents(50_000));
        transactionManager.execute(() -> {
            accountRepository.create(account);
            categoryRepository.create(new Category(CATEGORY_ID, USER_ID, "Rent", CategoryType.EXPENSE));
            obligationRepository.create(new Obligation(OBLIGATION_ID, USER_ID, "Rent", Money.ofCents(10_000),
                ACCOUNT_ID, CATEGORY_ID, new Recurrence(Frequency.WEEKLY, LocalDate.of(2026, 8, 6), null),
                ObligationStatus.ACTIVE));
        });
    }

    private PayOccurrence payOccurrence(OccurrenceResolutionRepository resolutions) {
        return new PayOccurrence(obligationRepository, resolutions,
            new ExpenseRegistration(accountRepository, categoryRepository, expenseRepository),
            transactionManager, CLOCK);
    }

    private CancelOperation cancelOperation(AccountRepository accounts, OccurrenceResolutionRepository resolutions) {
        return new CancelOperation(accounts, incomeRepository, expenseRepository, reversalRepository, resolutions,
            transactionManager, CLOCK);
    }

    private UUID pay(PayOccurrence payOccurrence) {
        return payOccurrence.execute(new PayOccurrenceCommand(USER_ID, OBLIGATION_ID, DUE_DATE, null, null, null,
            null));
    }

    private void cancel(CancelOperation cancelOperation, UUID expenseId) {
        cancelOperation.execute(new CancelOperationCommand(USER_ID, expenseId));
    }

    private long count(String table) throws Exception {
        return queryLong("SELECT COUNT(*) FROM " + table);
    }

    private long balance() throws Exception {
        return queryLong("SELECT balance FROM accounts WHERE id = '" + ACCOUNT_ID + "'");
    }

    private long queryLong(String sql) throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private Optional<OccurrenceResolution> resolution() {
        return transactionManager.execute(() ->
            resolutionRepository.findByObligationIdAndDueDate(OBLIGATION_ID, DUE_DATE));
    }

    private static String conflictingInsert(UUID resolutionId) {
        return "INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, resolved_at) "
            + "VALUES ('" + resolutionId + "', '" + OBLIGATION_ID + "', '" + DUE_DATE + "', 'SKIPPED', NULL, "
            + "'2026-08-01T00:00:00Z')";
    }

    private void insertConflictingResolutionOutsideTransaction(UUID resolutionId) throws Exception {
        try (Connection conn = connectionProvider.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(conflictingInsert(resolutionId));
        }
    }

    @Test
    void payThenCancelRestoresBalanceDeletesResolutionAndAllowsPayingAgain() throws Exception {
        PayOccurrence payOccurrence = payOccurrence(resolutionRepository);
        CancelOperation cancelOperation = cancelOperation(accountRepository, resolutionRepository);

        UUID expenseId = pay(payOccurrence);

        Expense expense = transactionManager.execute(() -> expenseRepository.findById(expenseId)).orElseThrow();
        assertEquals(Money.ofCents(10_000), expense.amount());
        assertEquals(TODAY, expense.operationDate());
        assertEquals(OperationStatus.ACTIVE, expense.status());
        assertEquals(40_000, balance());
        OccurrenceResolution paid = resolution().orElseThrow();
        assertEquals(ResolutionStatus.PAID, paid.status());
        assertEquals(Optional.of(expenseId), paid.expenseId());
        assertEquals(NOW, paid.resolvedAt());

        cancel(cancelOperation, expenseId);

        assertTrue(resolution().isEmpty());
        assertEquals(0, count("occurrence_resolutions"));
        assertEquals(1, count("reversals"));
        assertEquals(1, queryLong("SELECT COUNT(*) FROM reversals WHERE original_operation_id = '" + expenseId + "'"));
        assertEquals(50_000, balance());
        assertEquals(OperationStatus.CANCELLED,
            transactionManager.execute(() -> expenseRepository.findById(expenseId)).orElseThrow().status());

        UUID secondExpenseId = pay(payOccurrence);

        assertEquals(40_000, balance());
        assertEquals(Optional.of(secondExpenseId), resolution().orElseThrow().expenseId());
        assertEquals(2, count("expense_operations"));
    }

    @Test
    void payRollsBackExpenseAndBalanceWhenAConflictingResolutionHiddenFromTheLookupAlreadyExists() throws Exception {
        UUID externalId = UUID.randomUUID();
        insertConflictingResolutionOutsideTransaction(externalId);
        PayOccurrence payOccurrence = payOccurrence(new ResolutionRepositoryWrapper(resolutionRepository) {
            @Override
            public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
                return Optional.empty();
            }
        });

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> pay(payOccurrence));

        assertEquals(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED, e.code());
        assertEquals(ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, e.getMessage());
        assertEquals(0, count("expense_operations"));
        assertEquals(50_000, balance());
        assertEquals(1, count("occurrence_resolutions"));
        assertEquals(externalId, resolution().orElseThrow().id());
        assertEquals(ResolutionStatus.SKIPPED, resolution().orElseThrow().status());
    }

    @Test
    void payRollsBackExpenseAndBalanceWhenAConflictingResolutionIsInsertedJustBeforeCreate() throws Exception {
        UUID externalId = UUID.randomUUID();
        PayOccurrence payOccurrence = payOccurrence(new ResolutionRepositoryWrapper(resolutionRepository) {
            @Override
            public OccurrenceResolution create(OccurrenceResolution resolution) {
                try (Statement stmt = connectionHolder.get().createStatement()) {
                    stmt.execute(conflictingInsert(externalId));
                } catch (SQLException ex) {
                    throw new IllegalStateException(ex);
                }
                return super.create(resolution);
            }
        });

        BusinessRuleViolationException e = assertThrows(BusinessRuleViolationException.class,
            () -> pay(payOccurrence));

        assertEquals(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED, e.code());
        assertEquals(ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, e.getMessage());
        assertEquals(0, count("expense_operations"));
        assertEquals(50_000, balance());
        assertEquals(0, count("occurrence_resolutions"));
    }

    @Test
    void failedCancellationKeepsPaidResolutionWhenDeletingTheResolutionFails() throws Exception {
        UUID expenseId = pay(payOccurrence(resolutionRepository));
        CancelOperation failing = cancelOperation(accountRepository, new ResolutionRepositoryWrapper(
            resolutionRepository) {
            @Override
            public void delete(UUID resolutionId) {
                throw new IllegalStateException("delete failed");
            }
        });

        assertThrows(IllegalStateException.class, () -> cancel(failing, expenseId));

        assertCancellationRolledBack(expenseId);
    }

    @Test
    void failedCancellationKeepsPaidResolutionWhenUpdatingTheAccountFails() throws Exception {
        UUID expenseId = pay(payOccurrence(resolutionRepository));
        AccountRepository failingAccounts = new AccountRepository() {
            @Override
            public Account create(Account account) {
                return accountRepository.create(account);
            }

            @Override
            public Optional<Account> findById(UUID id) {
                return accountRepository.findById(id);
            }

            @Override
            public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
                return accountRepository.findByIdAndUserId(id, userId);
            }

            @Override
            public List<Account> findByUserId(UUID userId) {
                return accountRepository.findByUserId(userId);
            }

            @Override
            public boolean existsByUserIdAndName(UUID userId, String name) {
                return accountRepository.existsByUserIdAndName(userId, name);
            }

            @Override
            public Account update(Account account) {
                throw new IllegalStateException("update failed");
            }
        };
        CancelOperation failing = cancelOperation(failingAccounts, resolutionRepository);

        assertThrows(IllegalStateException.class, () -> cancel(failing, expenseId));

        assertCancellationRolledBack(expenseId);
    }

    private void assertCancellationRolledBack(UUID expenseId) throws Exception {
        OccurrenceResolution kept = resolution().orElseThrow();
        assertEquals(ResolutionStatus.PAID, kept.status());
        assertEquals(Optional.of(expenseId), kept.expenseId());
        assertEquals(OperationStatus.ACTIVE,
            transactionManager.execute(() -> expenseRepository.findById(expenseId)).orElseThrow().status());
        assertEquals(0, count("reversals"));
        assertEquals(40_000, balance());
        assertFalse(transactionManager.execute(() -> resolutionRepository.findByExpenseId(expenseId)).isEmpty());
    }

    @Test
    void cancellingAnExpenseWithoutResolutionKeepsOtherResolutions() throws Exception {
        UUID paidExpenseId = pay(payOccurrence(resolutionRepository));
        UUID otherExpenseId = UUID.randomUUID();
        transactionManager.execute(() -> expenseRepository.create(
            new Expense(otherExpenseId, USER_ID, Money.ofCents(1_000), TODAY, ACCOUNT_ID, CATEGORY_ID)));

        cancel(cancelOperation(accountRepository, resolutionRepository), otherExpenseId);

        assertEquals(Optional.of(paidExpenseId), resolution().orElseThrow().expenseId());
        assertEquals(1, count("occurrence_resolutions"));
        assertEquals(1, count("reversals"));
    }

    private abstract static class ResolutionRepositoryWrapper implements OccurrenceResolutionRepository {
        private final OccurrenceResolutionRepository delegate;

        ResolutionRepositoryWrapper(OccurrenceResolutionRepository delegate) {
            this.delegate = delegate;
        }

        @Override
        public OccurrenceResolution create(OccurrenceResolution resolution) {
            return delegate.create(resolution);
        }

        @Override
        public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
            return delegate.findByObligationId(obligationId);
        }

        @Override
        public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
            return delegate.findByObligationIdAndDueDate(obligationId, dueDate);
        }

        @Override
        public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
            return delegate.findByExpenseId(expenseId);
        }

        @Override
        public void delete(UUID resolutionId) {
            delegate.delete(resolutionId);
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
