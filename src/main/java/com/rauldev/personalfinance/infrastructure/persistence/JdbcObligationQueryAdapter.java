package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.ObligationQueryPort;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOccurrence;
import com.rauldev.personalfinance.application.readmodel.OccurrenceExpense;
import com.rauldev.personalfinance.application.readmodel.OccurrenceStatus;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

/**
 * Reads obligations with the names of their account and category.
 *
 * <p>The account and category are joined with {@code LEFT JOIN} and scoped to the obligation's user: a reference
 * that does not belong to the same user leaves the name {@code NULL}, and the row fails as corrupted data instead
 * of silently disappearing from the result. The expense of a paid resolution is scoped the same way, through the
 * user of its account.
 */
public final class JdbcObligationQueryAdapter implements ObligationQueryPort {
    private static final String SELECT_SQL = "SELECT o.id, o.name, o.amount, o.account_id, a.name AS account_name, "
        + "o.category_id, c.name AS category_name, o.frequency, o.start_date, o.end_date, o.status, o.archived_on "
        + "FROM obligations o "
        + "LEFT JOIN accounts a ON a.id = o.account_id AND a.user_id = o.user_id "
        + "LEFT JOIN categories c ON c.id = o.category_id AND c.user_id = o.user_id ";

    private static final String RESOLUTIONS_BY_USER_SQL =
        "SELECT r.id, r.obligation_id, r.due_date "
            + "FROM occurrence_resolutions r "
            + "JOIN obligations o ON o.id = r.obligation_id "
            + "WHERE o.user_id = ? "
            + "ORDER BY r.obligation_id, r.due_date";

    private static final String RESOLVED_OCCURRENCES_SQL =
        "SELECT r.id, r.due_date, r.status, r.expense_id, e.id AS expense_row_id, e.amount AS expense_amount, "
            + "e.operation_date AS expense_operation_date, a.id AS expense_account_id "
            + "FROM occurrence_resolutions r "
            + "JOIN obligations o ON o.id = r.obligation_id "
            + "LEFT JOIN expense_operations e ON e.id = r.expense_id "
            + "LEFT JOIN accounts a ON a.id = e.account_id AND a.user_id = o.user_id "
            + "WHERE r.obligation_id = ? AND o.user_id = ? "
            + "ORDER BY r.due_date DESC";

    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcObligationQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public List<ObligationDetails> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = SELECT_SQL + "WHERE o.user_id = ? ORDER BY o.name, o.id";
        return withConnection("Failed to query obligations", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ObligationDetails> obligations = new ArrayList<>();
                    while (resultSet.next()) {
                        obligations.add(mapRow(resultSet));
                    }
                    return obligations;
                }
            }
        });
    }

    @Override
    public Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId) {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = SELECT_SQL + "WHERE o.id = ? AND o.user_id = ?";
        return withConnection("Failed to query obligation details", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, obligationId.toString());
                statement.setString(2, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(mapRow(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    @Override
    public List<ResolutionSnapshot> findResolutionsByUserId(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("Failed to query obligation resolutions", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(RESOLUTIONS_BY_USER_SQL)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ResolutionSnapshot> resolutions = new ArrayList<>();
                    while (resultSet.next()) {
                        resolutions.add(mapResolution(resultSet));
                    }
                    return resolutions;
                }
            }
        });
    }

    @Override
    public List<ObligationOccurrence> findResolvedOccurrences(UUID obligationId, UUID userId) {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("Failed to query resolved occurrences", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(RESOLVED_OCCURRENCES_SQL)) {
                statement.setString(1, obligationId.toString());
                statement.setString(2, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ObligationOccurrence> occurrences = new ArrayList<>();
                    while (resultSet.next()) {
                        occurrences.add(mapResolvedOccurrence(resultSet));
                    }
                    return occurrences;
                }
            }
        });
    }

    private <T> T withConnection(String failureMessage, ConnectionWork<T> work) {
        try {
            if (connectionHolder.hasActiveTransaction()) {
                return work.apply(connectionHolder.get());
            }
            try (Connection connection = connectionProvider.getConnection()) {
                return work.apply(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(failureMessage, e);
        }
    }

    private static ObligationDetails mapRow(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            String name = resultSet.getString("name");
            Money amount = Money.ofCents(resultSet.getLong("amount"));
            AccountSummary account = new AccountSummary(
                UUID.fromString(resultSet.getString("account_id")), resultSet.getString("account_name"));
            CategorySummary category = new CategorySummary(
                UUID.fromString(resultSet.getString("category_id")), resultSet.getString("category_name"));
            Frequency frequency = Frequency.valueOf(resultSet.getString("frequency"));
            LocalDate startDate = LocalDate.parse(resultSet.getString("start_date"));
            String endDateValue = resultSet.getString("end_date");
            LocalDate endDate = endDateValue == null ? null : LocalDate.parse(endDateValue);
            ObligationStatus status = ObligationStatus.valueOf(resultSet.getString("status"));
            String archivedOnValue = resultSet.getString("archived_on");
            LocalDate archivedOn = archivedOnValue == null ? null : LocalDate.parse(archivedOnValue);

            return new ObligationDetails(id, name, amount, account, category,
                new Recurrence(frequency, startDate, endDate), status, archivedOn);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("obligations", rowId, e);
        }
    }

    private static ResolutionSnapshot mapResolution(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID obligationId = UUID.fromString(resultSet.getString("obligation_id"));
            LocalDate dueDate = LocalDate.parse(resultSet.getString("due_date"));

            return new ResolutionSnapshot(obligationId, dueDate);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("occurrence_resolutions", rowId, e);
        }
    }

    /**
     * A paid resolution whose expense is missing or belongs to another user's account fails as corrupted data, and
     * so does a skipped one with an expense.
     */
    private static ObligationOccurrence mapResolvedOccurrence(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            LocalDate dueDate = LocalDate.parse(resultSet.getString("due_date"));
            ResolutionStatus status = ResolutionStatus.valueOf(resultSet.getString("status"));
            String expenseIdValue = resultSet.getString("expense_id");

            return switch (status) {
                case SKIPPED -> {
                    if (expenseIdValue != null) {
                        throw new IllegalArgumentException("Skipped resolution cannot have an expense id");
                    }
                    yield new ObligationOccurrence(dueDate, OccurrenceStatus.SKIPPED, null);
                }
                case PAID -> {
                    Objects.requireNonNull(resultSet.getString("expense_row_id"), "Paid expense is missing");
                    Objects.requireNonNull(resultSet.getString("expense_account_id"),
                        "Paid expense does not belong to the obligation user");
                    OccurrenceExpense expense = new OccurrenceExpense(
                        UUID.fromString(expenseIdValue),
                        Money.ofCents(resultSet.getLong("expense_amount")),
                        LocalDate.parse(resultSet.getString("expense_operation_date")));
                    yield new ObligationOccurrence(dueDate, OccurrenceStatus.PAID, expense);
                }
            };
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("occurrence_resolutions", rowId, e);
        }
    }

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T apply(Connection connection) throws SQLException;
    }
}
