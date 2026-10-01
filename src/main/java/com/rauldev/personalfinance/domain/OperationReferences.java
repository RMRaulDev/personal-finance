package com.rauldev.personalfinance.domain;

import java.util.Objects;

/**
 * Validates the account and category that a new income, expense or obligation will reference. Check order: required
 * references, same user, category type, account active, category active.
 */
final class OperationReferences {

    private OperationReferences() {
    }

    static void validate(Account account, Category category, CategoryType expectedType, String invalidTypeMessage) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");
        if (!account.userId().equals(category.userId())) {
            throw new IllegalArgumentException("Account and category must belong to the same user");
        }
        if (category.type() != expectedType) {
            throw new IllegalArgumentException(invalidTypeMessage);
        }
        if (account.status() != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active");
        }
        if (category.status() != CategoryStatus.ACTIVE) {
            throw new BusinessRuleViolationException(BusinessRuleCode.CATEGORY_INACTIVE, "Category must be active");
        }
    }
}
