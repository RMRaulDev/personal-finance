package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.ObligationSummaryResponse;

/**
 * One pending occurrence within the dashboard horizon. {@code paymentBlocked} is {@code true} when
 * the obligation's account or category is inactive.
 */
public record UpcomingCommitmentResponse(
    ObligationSummaryResponse obligation,
    LocalDate dueDate,
    long amountCents,
    AccountSummaryResponse account,
    boolean paymentBlocked
) {

    public static UpcomingCommitmentResponse from(UpcomingCommitment commitment) {
        Objects.requireNonNull(commitment, "Upcoming commitment cannot be null");
        return new UpcomingCommitmentResponse(
            ObligationSummaryResponse.from(commitment.obligation()),
            commitment.dueDate(),
            MoneyCents.toCents(commitment.amount()),
            AccountSummaryResponse.from(commitment.account()),
            commitment.paymentBlocked());
    }
}
