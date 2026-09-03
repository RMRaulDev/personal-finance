package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcFinancialOperationQueryAdapter implements FinancialOperationQueryPort {
    private final SQLiteConnectionProvider connectionProvider;
    private final TransactionConnectionHolder connectionHolder;

    public JdbcFinancialOperationQueryAdapter(
        SQLiteConnectionProvider connectionProvider,
        TransactionConnectionHolder connectionHolder
    ) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider cannot be null");
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public List<FinancialOperationHistoryItem> search(OperationSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "Criteria cannot be null");

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeSearch(connection, criteria);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeSearch(connection, criteria);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute financial operation search", e);
            }
        }
    }

    @Override
    public Optional<FinancialOperationDetails> findDetailByIdAndUserId(UUID operationId, UUID userId) {
        Objects.requireNonNull(operationId, "Operation id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        if (connectionHolder.hasActiveTransaction()) {
            Connection connection = connectionHolder.get();
            return executeFindDetail(connection, operationId, userId);
        } else {
            try (Connection connection = connectionProvider.getConnection()) {
                return executeFindDetail(connection, operationId, userId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to query financial operation details", e);
            }
        }
    }

    private List<FinancialOperationHistoryItem> executeSearch(Connection connection, OperationSearchCriteria criteria) {
        boolean includeIncome = (criteria.operationType() == null || criteria.operationType() == OperationType.INCOME);
        boolean includeExpense = (criteria.operationType() == null || criteria.operationType() == OperationType.EXPENSE);
        boolean includeTransfer = (criteria.operationType() == null || criteria.operationType() == OperationType.TRANSFER)
            && criteria.categoryId() == null;

        if (!includeIncome && !includeExpense && !includeTransfer) {
            return Collections.emptyList();
        }

        List<String> branches = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (includeIncome) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT io.id AS op_id, 'INCOME' AS op_type, io.amount AS amount, io.operation_date AS operation_date, ")
              .append("io.status AS status, r.cancelled_at AS cancelled_at, ")
              .append("a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, ")
              .append("NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name ")
              .append("FROM income_operations io ")
              .append("JOIN accounts a ON a.id = io.account_id ")
              .append("JOIN categories c ON c.id = io.category_id ")
              .append("LEFT JOIN reversals r ON r.original_operation_id = io.id ")
              .append("WHERE a.user_id = ? ");
            params.add(criteria.userId().toString());

            if (criteria.accountId() != null) {
                sb.append("AND io.account_id = ? ");
                params.add(criteria.accountId().toString());
            }
            if (criteria.categoryId() != null) {
                sb.append("AND io.category_id = ? ");
                params.add(criteria.categoryId().toString());
            }
            if (criteria.from() != null) {
                sb.append("AND io.operation_date >= ? ");
                params.add(criteria.from().toString());
            }
            if (criteria.to() != null) {
                sb.append("AND io.operation_date <= ? ");
                params.add(criteria.to().toString());
            }
            branches.add(sb.toString());
        }

        if (includeExpense) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT eo.id AS op_id, 'EXPENSE' AS op_type, eo.amount AS amount, eo.operation_date AS operation_date, ")
              .append("eo.status AS status, r.cancelled_at AS cancelled_at, ")
              .append("a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, ")
              .append("NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name ")
              .append("FROM expense_operations eo ")
              .append("JOIN accounts a ON a.id = eo.account_id ")
              .append("JOIN categories c ON c.id = eo.category_id ")
              .append("LEFT JOIN reversals r ON r.original_operation_id = eo.id ")
              .append("WHERE a.user_id = ? ");
            params.add(criteria.userId().toString());

            if (criteria.accountId() != null) {
                sb.append("AND eo.account_id = ? ");
                params.add(criteria.accountId().toString());
            }
            if (criteria.categoryId() != null) {
                sb.append("AND eo.category_id = ? ");
                params.add(criteria.categoryId().toString());
            }
            if (criteria.from() != null) {
                sb.append("AND eo.operation_date >= ? ");
                params.add(criteria.from().toString());
            }
            if (criteria.to() != null) {
                sb.append("AND eo.operation_date <= ? ");
                params.add(criteria.to().toString());
            }
            branches.add(sb.toString());
        }

        if (includeTransfer) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT to_op.id AS op_id, 'TRANSFER' AS op_type, to_op.amount AS amount, to_op.operation_date AS operation_date, ")
              .append("NULL AS status, NULL AS cancelled_at, ")
              .append("NULL AS account_id, NULL AS account_name, NULL AS category_id, NULL AS category_name, ")
              .append("sa.id AS source_account_id, sa.name AS source_account_name, ta.id AS target_account_id, ta.name AS target_account_name ")
              .append("FROM transfer_operations to_op ")
              .append("JOIN accounts sa ON sa.id = to_op.source_account_id ")
              .append("JOIN accounts ta ON ta.id = to_op.target_account_id ")
              .append("WHERE sa.user_id = ? ");
            params.add(criteria.userId().toString());

            if (criteria.accountId() != null) {
                sb.append("AND (to_op.source_account_id = ? OR to_op.target_account_id = ?) ");
                params.add(criteria.accountId().toString());
                params.add(criteria.accountId().toString());
            }
            if (criteria.from() != null) {
                sb.append("AND to_op.operation_date >= ? ");
                params.add(criteria.from().toString());
            }
            if (criteria.to() != null) {
                sb.append("AND to_op.operation_date <= ? ");
                params.add(criteria.to().toString());
            }
            branches.add(sb.toString());
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT op_id, op_type, amount, operation_date, status, cancelled_at, ")
                  .append("account_id, account_name, category_id, category_name, ")
                  .append("source_account_id, source_account_name, target_account_id, target_account_name ")
                  .append("FROM (")
                  .append(String.join(" UNION ALL ", branches))
                  .append(") ORDER BY operation_date DESC, op_id DESC LIMIT ? OFFSET ?");

        params.add(criteria.pageSize());
        params.add((criteria.page() - 1) * criteria.pageSize());

        try (PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer intVal) {
                    statement.setInt(i + 1, intVal);
                } else if (param instanceof String strVal) {
                    statement.setString(i + 1, strVal);
                }
            }

            try (ResultSet rs = statement.executeQuery()) {
                List<FinancialOperationHistoryItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapHistoryItem(rs));
                }
                return items;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute financial operation search", e);
        }
    }

    private Optional<FinancialOperationDetails> executeFindDetail(Connection connection, UUID operationId, UUID userId) {
        String sql = "SELECT op_id, op_type, amount, operation_date, status, cancelled_at, "
            + "account_id, account_name, category_id, category_name, "
            + "source_account_id, source_account_name, target_account_id, target_account_name "
            + "FROM ("
            + "SELECT io.id AS op_id, 'INCOME' AS op_type, io.amount AS amount, io.operation_date AS operation_date, "
            + "io.status AS status, r.cancelled_at AS cancelled_at, "
            + "a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, "
            + "NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name "
            + "FROM income_operations io "
            + "JOIN accounts a ON a.id = io.account_id "
            + "JOIN categories c ON c.id = io.category_id "
            + "LEFT JOIN reversals r ON r.original_operation_id = io.id "
            + "WHERE io.id = ? AND a.user_id = ? "
            + "UNION ALL "
            + "SELECT eo.id AS op_id, 'EXPENSE' AS op_type, eo.amount AS amount, eo.operation_date AS operation_date, "
            + "eo.status AS status, r.cancelled_at AS cancelled_at, "
            + "a.id AS account_id, a.name AS account_name, c.id AS category_id, c.name AS category_name, "
            + "NULL AS source_account_id, NULL AS source_account_name, NULL AS target_account_id, NULL AS target_account_name "
            + "FROM expense_operations eo "
            + "JOIN accounts a ON a.id = eo.account_id "
            + "JOIN categories c ON c.id = eo.category_id "
            + "LEFT JOIN reversals r ON r.original_operation_id = eo.id "
            + "WHERE eo.id = ? AND a.user_id = ? "
            + "UNION ALL "
            + "SELECT to_op.id AS op_id, 'TRANSFER' AS op_type, to_op.amount AS amount, to_op.operation_date AS operation_date, "
            + "NULL AS status, NULL AS cancelled_at, "
            + "NULL AS account_id, NULL AS account_name, NULL AS category_id, NULL AS category_name, "
            + "sa.id AS source_account_id, sa.name AS source_account_name, ta.id AS target_account_id, ta.name AS target_account_name "
            + "FROM transfer_operations to_op "
            + "JOIN accounts sa ON sa.id = to_op.source_account_id "
            + "JOIN accounts ta ON ta.id = to_op.target_account_id "
            + "WHERE to_op.id = ? AND sa.user_id = ?"
            + ")";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            String opIdStr = operationId.toString();
            String userIdStr = userId.toString();

            statement.setString(1, opIdStr);
            statement.setString(2, userIdStr);
            statement.setString(3, opIdStr);
            statement.setString(4, userIdStr);
            statement.setString(5, opIdStr);
            statement.setString(6, userIdStr);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapDetails(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query financial operation details", e);
        }
    }

    private static FinancialOperationHistoryItem mapHistoryItem(ResultSet rs) throws SQLException {
        UUID opId = UUID.fromString(rs.getString("op_id"));
        OperationType opType = OperationType.valueOf(rs.getString("op_type"));
        Money amount = Money.ofCents(rs.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(rs.getString("operation_date"));

        String statusStr = rs.getString("status");
        OperationStatus status = statusStr != null ? OperationStatus.valueOf(statusStr) : null;

        String cancelledAtStr = rs.getString("cancelled_at");
        Instant cancelledAt = cancelledAtStr != null ? Instant.parse(cancelledAtStr) : null;

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

        return new FinancialOperationHistoryItem(
            opId, opType, amount, operationDate, status, cancelledAt, account, category, transfer
        );
    }

    private static FinancialOperationDetails mapDetails(ResultSet rs) throws SQLException {
        UUID opId = UUID.fromString(rs.getString("op_id"));
        OperationType opType = OperationType.valueOf(rs.getString("op_type"));
        Money amount = Money.ofCents(rs.getLong("amount"));
        LocalDate operationDate = LocalDate.parse(rs.getString("operation_date"));

        String statusStr = rs.getString("status");
        OperationStatus status = statusStr != null ? OperationStatus.valueOf(statusStr) : null;

        String cancelledAtStr = rs.getString("cancelled_at");
        Instant cancelledAt = cancelledAtStr != null ? Instant.parse(cancelledAtStr) : null;

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

        return new FinancialOperationDetails(
            opId, opType, amount, operationDate, status, cancelledAt, account, category, transfer
        );
    }
}
