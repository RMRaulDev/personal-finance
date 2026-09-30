package com.rauldev.personalfinance.domain;

/**
 * Stable, client-facing identifiers of business rules. Names are part of the public API contract
 * and must not be renamed.
 */
public enum BusinessRuleCode {
    INSUFFICIENT_BALANCE,
    ACCOUNT_INACTIVE,
    OPERATION_ALREADY_CANCELLED,
    ACCOUNT_NAME_ALREADY_EXISTS,
    CATEGORY_NAME_ALREADY_EXISTS
}
