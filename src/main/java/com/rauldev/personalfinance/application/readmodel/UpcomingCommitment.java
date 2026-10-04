package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.domain.Money;

/**
 * One pending occurrence within the horizon.
 *
 * @param paymentBlocked whether the account or the category of the obligation is inactive
 */
public record UpcomingCommitment(
    ObligationSummary obligation,
    LocalDate dueDate,
    Money amount,
    AccountSummary account,
    boolean paymentBlocked
) {
    public UpcomingCommitment {
        Objects.requireNonNull(obligation, "Obligation cannot be null");
        Objects.requireNonNull(dueDate, "Due date cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(account, "Account cannot be null");
    }
}
