package com.rauldev.personalfinance.domain;

import java.util.Objects;

/**
 * Signals that a request is well-formed but violates a business rule given the current state of an
 * aggregate. It deliberately does not extend {@link IllegalStateException}, which is reserved for
 * technical faults.
 */
public final class BusinessRuleViolationException extends RuntimeException {
    private final BusinessRuleCode code;

    public BusinessRuleViolationException(BusinessRuleCode code, String message) {
        super(Objects.requireNonNull(message, "Business rule message cannot be null"));
        this.code = Objects.requireNonNull(code, "Business rule code cannot be null");
    }

    public BusinessRuleCode code() {
        return code;
    }
}
