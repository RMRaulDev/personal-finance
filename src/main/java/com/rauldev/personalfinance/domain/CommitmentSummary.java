package com.rauldev.personalfinance.domain;

import java.util.List;
import java.util.Objects;

/**
 * Result of {@link CommitmentCalculator}. {@code attention} is complete and ordered by priority.
 */
public record CommitmentSummary(Money committedAmount, Money balance, Money availableToSpend, Money shortfall,
                                List<ObligationCommitment> obligations, List<AccountCommitment> accounts,
                                List<Attention> attention) {

    public CommitmentSummary {
        Objects.requireNonNull(committedAmount, "Committed amount cannot be null");
        Objects.requireNonNull(balance, "Balance cannot be null");
        Objects.requireNonNull(availableToSpend, "Available to spend cannot be null");
        Objects.requireNonNull(shortfall, "Shortfall cannot be null");
        obligations = List.copyOf(Objects.requireNonNull(obligations, "Obligations cannot be null"));
        accounts = List.copyOf(Objects.requireNonNull(accounts, "Accounts cannot be null"));
        attention = List.copyOf(Objects.requireNonNull(attention, "Attention cannot be null"));
    }
}
