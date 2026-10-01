package com.rauldev.personalfinance.domain;

/**
 * Stable, client-facing identifiers of business rules. Names are part of the public API contract
 * and must not be renamed.
 */
public enum BusinessRuleCode {
    INSUFFICIENT_BALANCE,
    ACCOUNT_INACTIVE,
    CATEGORY_INACTIVE,
    OPERATION_ALREADY_CANCELLED,
    ACCOUNT_NAME_ALREADY_EXISTS,
    CATEGORY_NAME_ALREADY_EXISTS,
    OBLIGATION_NAME_ALREADY_EXISTS,
    OBLIGATION_ARCHIVED,
    OBLIGATION_HAS_OVERDUE_OCCURRENCES,
    OCCURRENCE_ALREADY_RESOLVED,
    OCCURRENCE_NOT_SCHEDULED,
    OBLIGATION_SCHEDULE_CONFLICTS_WITH_RESOLUTION
}
