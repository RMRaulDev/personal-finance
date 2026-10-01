package com.rauldev.personalfinance.infrastructure.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.domain.OccurrenceResolution;
import com.rauldev.personalfinance.domain.ResolutionStatus;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

public final class JdbcOccurrenceResolutionRepository implements OccurrenceResolutionRepository {
    // Matches SQLite's "UNIQUE constraint failed: occurrence_resolutions.obligation_id, occurrence_resolutions.due_date"
    // message for the UNIQUE (obligation_id, due_date) constraint in schema.sql. The UNIQUE (expense_id) constraint
    // is a different one and is not translated. Changing the constraint requires updating this value.
    private static final String DUPLICATE_DUE_DATE_CONSTRAINT_COLUMNS =
        "occurrence_resolutions.obligation_id, occurrence_resolutions.due_date";

    private static final String SELECT_COLUMNS =
        "SELECT id, obligation_id, due_date, status, expense_id, resolved_at FROM occurrence_resolutions ";

    private final TransactionConnectionHolder connectionHolder;

    public JdbcOccurrenceResolutionRepository(TransactionConnectionHolder connectionHolder) {
        this.connectionHolder = Objects.requireNonNull(connectionHolder, "Connection holder cannot be null");
    }

    @Override
    public OccurrenceResolution create(OccurrenceResolution resolution) {
        Objects.requireNonNull(resolution, "Resolution cannot be null");

        String sql = "INSERT INTO occurrence_resolutions (id, obligation_id, due_date, status, expense_id, resolved_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resolution.id().toString());
            statement.setString(2, resolution.obligationId().toString());
            statement.setString(3, resolution.dueDate().toString());
            statement.setString(4, resolution.status().name());
            statement.setString(5, resolution.expenseId().map(UUID::toString).orElse(null));
            statement.setString(6, resolution.resolvedAt().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (SqliteConstraintViolations.isUniqueViolation(e, DUPLICATE_DUE_DATE_CONSTRAINT_COLUMNS)) {
                throw new BusinessRuleViolationException(BusinessRuleCode.OCCURRENCE_ALREADY_RESOLVED,
                    ApplicationConstants.OCCURRENCE_ALREADY_RESOLVED_MESSAGE, e);
            }
            throw new RuntimeException("Failed to create occurrence resolution", e);
        }

        return resolution;
    }

    @Override
    public List<OccurrenceResolution> findByObligationId(UUID obligationId) {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");

        String sql = SELECT_COLUMNS + "WHERE obligation_id = ? ORDER BY due_date";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, obligationId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<OccurrenceResolution> resolutions = new ArrayList<>();
                while (resultSet.next()) {
                    resolutions.add(mapRow(resultSet));
                }
                return resolutions;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find occurrence resolutions by obligation id", e);
        }
    }

    @Override
    public Optional<OccurrenceResolution> findByObligationIdAndDueDate(UUID obligationId, LocalDate dueDate) {
        Objects.requireNonNull(obligationId, "Obligation id cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");

        String sql = SELECT_COLUMNS + "WHERE obligation_id = ? AND due_date = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, obligationId.toString());
            statement.setString(2, dueDate.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find occurrence resolution by obligation id and due date", e);
        }
    }

    @Override
    public Optional<OccurrenceResolution> findByExpenseId(UUID expenseId) {
        Objects.requireNonNull(expenseId, "Expense id cannot be null");

        String sql = SELECT_COLUMNS + "WHERE expense_id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expenseId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find occurrence resolution by expense id", e);
        }
    }

    @Override
    public void delete(UUID resolutionId) {
        Objects.requireNonNull(resolutionId, "Resolution id cannot be null");

        String sql = "DELETE FROM occurrence_resolutions WHERE id = ?";
        Connection connection = connectionHolder.get();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, resolutionId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete occurrence resolution", e);
        }
    }

    private static OccurrenceResolution mapRow(ResultSet resultSet) throws SQLException {
        String rowId = resultSet.getString("id");
        try {
            UUID id = UUID.fromString(resultSet.getString("id"));
            UUID obligationId = UUID.fromString(resultSet.getString("obligation_id"));
            LocalDate dueDate = LocalDate.parse(resultSet.getString("due_date"));
            ResolutionStatus status = ResolutionStatus.valueOf(resultSet.getString("status"));
            String expenseIdValue = resultSet.getString("expense_id");
            UUID expenseId = expenseIdValue == null ? null : UUID.fromString(expenseIdValue);
            Instant resolvedAt = Instant.parse(resultSet.getString("resolved_at"));

            return new OccurrenceResolution(id, obligationId, dueDate, status, expenseId, resolvedAt);
        } catch (RuntimeException e) {
            throw new CorruptedPersistedDataException("occurrence_resolutions", rowId, e);
        }
    }
}
