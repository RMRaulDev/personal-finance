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
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

/**
 * Reads obligations with the names of their account and category.
 *
 * <p>The account and category are joined with {@code LEFT JOIN} and scoped to the obligation's user: a reference
 * that does not belong to the same user leaves the name {@code NULL}, and the row fails as corrupted data instead
 * of silently disappearing from the result.
 */
public final class JdbcObligationQueryAdapter implements ObligationQueryPort {
    private static final String SELECT_SQL = "SELECT o.id, o.name, o.amount, o.account_id, a.name AS account_name, "
        + "o.category_id, c.name AS category_name, o.frequency, o.start_date, o.end_date, o.status "
        + "FROM obligations o "
        + "LEFT JOIN accounts a ON a.id = o.account_id AND a.user_id = o.user_id "
        + "LEFT JOIN categories c ON c.id = o.category_id AND c.user_id = o.user_id ";

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

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeListQuery(connection, sql, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeListQuery(connection, sql, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query obligations", e);
            }
        }
    }

    @Override
    public Optional<ObligationDetails> findByIdAndUserId(UUID obligationId, UUID userId) {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = SELECT_SQL + "WHERE o.id = ? AND o.user_id = ?";

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeQuery(connection, sql, obligationId, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeQuery(connection, sql, obligationId, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query obligation details", e);
            }
        }
    }

    private List<ObligationDetails> executeListQuery(Connection connection, String sql, UUID userId) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ObligationDetails> obligations = new ArrayList<>();
                while (resultSet.next()) {
                    obligations.add(mapRow(resultSet));
                }
                return obligations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query obligations", e);
        }
    }

    private Optional<ObligationDetails> executeQuery(
        Connection connection,
        String sql,
        UUID obligationId,
        UUID userId
    ) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, obligationId.toString());
            statement.setString(2, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query obligation details", e);
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

            return new ObligationDetails(
                id, name, amount, account, category, new Recurrence(frequency, startDate, endDate), status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("obligations", rowId, e);
        }
    }
}
