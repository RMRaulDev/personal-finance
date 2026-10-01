package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Obligation;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcObligationRepository implements ObligationRepository {
    // Matches SQLite's "UNIQUE constraint failed: obligations.user_id, obligations.name" message for the
    // UNIQUE (user_id, name) constraint in schema.sql. Changing that constraint requires updating this value.
    private static final String DUPLICATE_NAME_CONSTRAINT_COLUMNS = "obligations.user_id, obligations.name";

    private static final String SELECT_COLUMNS =
        "SELECT id, user_id, name, amount, account_id, category_id, frequency, start_date, end_date, status "
            + "FROM obligations ";

    private final TransactionConnectionHolder connectionHolder;

    public JdbcObligationRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public Obligation create(Obligation obligation) {
        Objects.requireNonNull(obligation, "Obligation cannot be null");

        String sql = "INSERT INTO obligations "
            + "(id, user_id, name, amount, account_id, category_id, frequency, start_date, end_date, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Recurrence recurrence = obligation.recurrence();
            statement.setString(1, obligation.id().toString());
            statement.setString(2, obligation.userId().toString());
            statement.setString(3, obligation.name());
            statement.setLong(4, toCents(obligation.amount()));
            statement.setString(5, obligation.accountId().toString());
            statement.setString(6, obligation.categoryId().toString());
            statement.setString(7, recurrence.frequency().name());
            statement.setString(8, recurrence.startDate().toString());
            statement.setString(9, recurrence.endDate().map(LocalDate::toString).orElse(null));
            statement.setString(10, obligation.status().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (SqliteConstraintViolations.isUniqueViolation(e, DUPLICATE_NAME_CONSTRAINT_COLUMNS)) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS,
                    ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, e);
            }
            throw new RuntimeException("Failed to create obligation", e);
        }

        return obligation;
    }

    @Override
    public Optional<Obligation> findByIdAndUserId(UUID id, UUID userId) {
        Objects.requireNonNull(id, "Obligation id cannot be null");
        Objects.requireNonNull(userId, "User id cannot be null");

        String sql = SELECT_COLUMNS + "WHERE id = ? AND user_id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.setString(2, userId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find obligation by id and user id", e);
        }
    }

    @Override
    public boolean existsByUserIdAndName(UUID userId, String name) {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Obligation name cannot be null");

        String sql = "SELECT 1 FROM obligations WHERE user_id = ? AND name = ? LIMIT 1";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check obligation existence by user id and name", e);
        }
    }

    @Override
    public boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID obligationId) {
        Objects.requireNonNull(userId, "User id cannot be null");
        Objects.requireNonNull(name, "Obligation name cannot be null");
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");

        String sql = "SELECT 1 FROM obligations WHERE user_id = ? AND name = ? AND id <> ? LIMIT 1";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId.toString());
            statement.setString(2, name);
            statement.setString(3, obligationId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check obligation existence by user id and name excluding id", e);
        }
    }

    @Override
    public Obligation update(Obligation obligation) {
        Objects.requireNonNull(obligation, "Obligation cannot be null");

        String sql = "UPDATE obligations SET name = ?, amount = ?, account_id = ?, category_id = ?, frequency = ?, "
            + "start_date = ?, end_date = ?, status = ? WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            Recurrence recurrence = obligation.recurrence();
            statement.setString(1, obligation.name());
            statement.setLong(2, toCents(obligation.amount()));
            statement.setString(3, obligation.accountId().toString());
            statement.setString(4, obligation.categoryId().toString());
            statement.setString(5, recurrence.frequency().name());
            statement.setString(6, recurrence.startDate().toString());
            statement.setString(7, recurrence.endDate().map(LocalDate::toString).orElse(null));
            statement.setString(8, obligation.status().name());
            statement.setString(9, obligation.id().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (SqliteConstraintViolations.isUniqueViolation(e, DUPLICATE_NAME_CONSTRAINT_COLUMNS)) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OBLIGATION_NAME_ALREADY_EXISTS,
                    ApplicationConstants.OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE, e);
            }
            throw new RuntimeException("Failed to update obligation", e);
        }

        return obligation;
    }

    private static Obligation mapRow(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            UUID userId = UUID.fromString(resultSet.getString("user_id"));
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
            return new Obligation(id, userId, name, amount, accountId, categoryId, recurrence, status);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("obligations", rowId, e);
        }
    }

    private static long toCents(Money money) {
        return money.amount().movePointRight(2).longValueExact();
    }
}
