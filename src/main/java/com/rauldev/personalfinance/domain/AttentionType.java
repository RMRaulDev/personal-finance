package com.rauldev.personalfinance.domain;

/**
 * Attention types, declared in priority order (highest first).
 */
public enum AttentionType {
    OVERDUE_OCCURRENCE,
    SHORTFALL,
    PAYMENT_BLOCKED,
    ACCOUNT_SHORTFALL
}
