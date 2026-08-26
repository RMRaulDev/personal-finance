package com.rauldev.personalfinance.application.query;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record OperationSearchCriteria(
    UUID userId,
    UUID accountId,
    UUID categoryId,
    OperationType operationType,
    LocalDate fromDate,
    LocalDate toDate
) {
    public OperationSearchCriteria {
        Objects.requireNonNull(userId, "User id cannot be null");
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be later than to date");
        }
    }

    public static OperationSearchCriteria forUser(UUID userId) {
        return new OperationSearchCriteria(userId, null, null, null, null, null);
    }
}
