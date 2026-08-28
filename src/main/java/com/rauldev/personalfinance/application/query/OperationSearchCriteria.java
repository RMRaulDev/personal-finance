package com.rauldev.personalfinance.application.query;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.ApplicationConstants;

public record OperationSearchCriteria(
    UUID userId,
    UUID accountId,
    UUID categoryId,
    OperationType operationType,
    LocalDate from,
    LocalDate to,
    int page,
    int pageSize
) {
    public OperationSearchCriteria {
        Objects.requireNonNull(userId, "User id cannot be null");
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be later than to date");
        }
        if (page < 1) {
            throw new IllegalArgumentException("Page must be greater than 0");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (pageSize > ApplicationConstants.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + ApplicationConstants.MAX_PAGE_SIZE);
        }
    }

    public static OperationSearchCriteria forUser(UUID userId) {
        return new OperationSearchCriteria(userId, null, null, null, null, null, 1, 20);
    }
}
