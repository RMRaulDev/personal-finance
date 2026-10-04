package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.DashboardQueryPort;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.AccountSnapshot;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.CategorySnapshot;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationSnapshot;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcDashboardQueryAdapter implements DashboardQueryPort {
    private static final String ACTIVE_OBLIGATIONS_SQL =
        "SELECT id, name, amount, account_id, category_id, frequency, start_date, end_date, status "
            + "FROM obligations WHERE user_id = ? AND status = 'ACTIVE' ORDER BY name, id";

    private static final String RESOLUTIONS_OF_ACTIVE_OBLIGATIONS_SQL =
        "SELECT r.id, r.obligation_id, r.due_date "
            + "FROM occurrence_resolutions r "
            + "JOIN obligations o ON o.id = r.obligation_id "
            + "WHERE o.user_id = ? AND o.status = 'ACTIVE' "
            + "ORDER BY r.obligation_id, r.due_date";

    private static final String ACCOUNTS_SQL =
        "SELECT id, name, balance, status FROM accounts WHERE user_id = ? ORDER BY name, id";

    private static final String CATEGORIES_SQL = "SELECT id, status FROM categories WHERE user_id = ?";

    private static final String RECENT_ACTIVITY_SQL =
        "SELECT op_id, op_type, amount, operation_date, "
            + "account_id, account_name, category_id, category_name, "
            + "source_account_id, source_account_name, target_account_id, target_account_name, "
            + "obligation_id, obligation_name "
            + "FROM ("
            + "SELECT io.id AS op_id, 'INCOME' AS op_type, io.amount AS amount, io.operation_date AS operation_date, "
            + "io.created_at AS created_at, "
            + "a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, "
            + "NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name, "
            + "NULL AS obligation_id, NULL AS obligation_name "
            + "FROM income_operations io "
            + "JOIN accounts a ON a.id = io.account_id "
            + "JOIN categories c ON c.id = io.category_id "
            + "WHERE a.user_id = ? AND io.status = 'ACTIVE' "
            + "UNION ALL "
            + "SELECT eo.id AS op_id, 'EXPENSE' AS op_type, eo.amount AS amount, eo.operation_date AS operation_date, "
            + "eo.created_at AS created_at, "
            + "a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, "
            + "NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name, "
            + "ob.id AS obligation_id, ob.name AS obligation_name "
            + "FROM expense_operations eo "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "JOIN categories c ON c.id = eo.category_id "
            + "LEFT JOIN occurrence_resolutions orr ON orr.expense_id = eo.id "
            + "LEFT JOIN obligations ob ON ob.id = orr.obligation_id AND ob.user_id = a.user_id "
            + "WHERE a.user_id = ? AND eo.status = 'ACTIVE' "
            + "UNION ALL "
            + "SELECT to_op.id AS op_id, 'TRANSFER' AS op_type, to_op.amount AS amount, to_op.operation_date AS operation_date, "
            + "to_op.created_at AS created_at, "
            + "NULL AS account_id, NULL AS account_name, NULL AS category_id, NULL AS category_name, "
            + "sa.id AS source_account_id, sa.name AS source_account_name, ta.id AS target_account_id, ta.name AS target_account_name, "
            + "NULL AS obligation_id, NULL AS obligation_name "
            + "FROM transfer_operations to_op "
            + "JOIN accounts sa ON sa.id = to_op.source_account_id "
            + "JOIN accounts ta ON ta.id = to_op.target_account_id "
            + "WHERE sa.user_id = ?"
            + ") ORDER BY operation_date DESC, created_at DESC, op_id DESC LIMIT ?";

    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcDashboardQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public List<ObligationSnapshot> findActiveObligations(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("obligations", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(ACTIVE_OBLIGATIONS_SQL)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<ObligationSnapshot> obligations = new ArrayList<>();
                    while (resultSet.next()) {
                        obligations.add(mapObligation(resultSet));
                    }
                    return obligations;
                }
            }
        });
    }

    @Override
    public List<ResolutionSnapshot> findResolutionsOfActiveObligations(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("resolutions", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(RESOLUTIONS_OF_ACTIVE_OBLIGATIONS_SQL)) {
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
    public List<AccountSnapshot> findAccounts(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("accounts", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(ACCOUNTS_SQL)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<AccountSnapshot> accounts = new ArrayList<>();
                    while (resultSet.next()) {
                        accounts.add(mapAccount(resultSet));
                    }
                    return accounts;
                }
            }
        });
    }

    @Override
    public List<CategorySnapshot> findCategories(UUID userId) {
        Objects.requireNonNull(userId, "User id cannot be null");

        return withConnection("categories", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(CATEGORIES_SQL)) {
                statement.setString(1, userId.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<CategorySnapshot> categories = new ArrayList<>();
                    while (resultSet.next()) {
                        categories.add(mapCategory(resultSet));
                    }
                    return categories;
                }
            }
        });
    }

    @Override
    public List<RecentActivityItem> findRecentActivity(UUID userId, int limit) {
        Objects.requireNonNull(userId, "User id cannot be null");
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        return withConnection("recent activity", connection -> {
            try (PreparedStatement statement = connection.prepareStatement(RECENT_ACTIVITY_SQL)) {
                String userIdValue = userId.toString();
                statement.setString(1, userIdValue);
                statement.setString(2, userIdValue);
                statement.setString(3, userIdValue);
                statement.setInt(4, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<RecentActivityItem> items = new ArrayList<>();
                    while (resultSet.next()) {
                        items.add(mapRecentActivityItem(resultSet));
                    }
                    return items;
                }
            }
        });
    }

    private <T> T withConnection(String part, ConnectionWork<T> work) {
        try {
            if (connectionHolder.hasActiveTransaction()) {
                return work.apply(connectionHolder.get());
            }
            try (Connection connection = connectionProvider.getConnection()) {
                return work.apply(connection);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query dashboard " + part, e);
        }
    }

    private static ObligationSnapshot mapObligation(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            String name = resultSet.getString("name");
            Money amount = Money.ofCents(resultSet.getLong("amount"));
            UUID accountId = UUID.fromString(resultSet.getString("account_id"));
            UUID categoryId = UUID.fromString(resultSet.getString("category_id"));
            Frequency frequency = Frequency.valueOf(resultSet.getString("frequency"));
            LocalDate startDate = LocalDate.parse(resultSet.getString("start_date"));
            String endDateValue = resultSet.getString("end_date");
            LocalDate endDate = endDateValue == null ? null : LocalDate.parse(endDateValue);
            ObligationStatus status = ObligationStatus.valueOf(resultSet.getString("status"));

            Recurrence recurrence = new Recurrence(frequency, startDate, endDate);
            return new ObligationSnapshot(id, name, amount, accountId, categoryId, recurrence, status);
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

    private static AccountSnapshot mapAccount(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            String name = resultSet.getString("name");
            Money balance = Money.ofCents(resultSet.getLong("balance"));
            AccountStatus status = AccountStatus.valueOf(resultSet.getString("status"));

            return new AccountSnapshot(id, name, balance, status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("accounts", rowId, e);
        }
    }

    private static CategorySnapshot mapCategory(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            CategoryStatus status = CategoryStatus.valueOf(resultSet.getString("status"));

            return new CategorySnapshot(id, status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("categories", rowId, e);
        }
    }

    private static RecentActivityItem mapRecentActivityItem(ResultSet rs) throws SQLException {
        String rowId = rs.getString("op_id");
        String opTypeValue = rs.getString("op_type");
        try {
            UUID opId = UUID.fromString(rs.getString("op_id"));
            OperationType opType = OperationType.valueOf(rs.getString("op_type"));
            Money amount = Money.ofCents(rs.getLong("amount"));
            LocalDate operationDate = LocalDate.parse(rs.getString("operation_date"));

            String accountIdStr = rs.getString("account_id");
            AccountSummary account = accountIdStr != null
                ? new AccountSummary(UUID.fromString(accountIdStr), rs.getString("account_name"))
                : null;

            String categoryIdStr = rs.getString("category_id");
            CategorySummary category = categoryIdStr != null
                ? new CategorySummary(UUID.fromString(categoryIdStr), rs.getString("category_name"))
                : null;

            String sourceAccountIdStr = rs.getString("source_account_id");
            TransferDetails transfer = sourceAccountIdStr != null
                ? new TransferDetails(
                    new AccountSummary(UUID.fromString(sourceAccountIdStr), rs.getString("source_account_name")),
                    new AccountSummary(UUID.fromString(rs.getString("target_account_id")), rs.getString("target_account_name"))
                )
                : null;

            String obligationIdStr = rs.getString("obligation_id");
            ObligationSummary obligation = obligationIdStr != null
                ? new ObligationSummary(UUID.fromString(obligationIdStr), rs.getString("obligation_name"))
                : null;

            return new RecentActivityItem(opId, opType, amount, operationDate, account, category, transfer, obligation);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("financial operations (" + opTypeValue + ")", rowId, e);
        }
    }

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T apply(Connection connection) throws SQLException;
    }
}
