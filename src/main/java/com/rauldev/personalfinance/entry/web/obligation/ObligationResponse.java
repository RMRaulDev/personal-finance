package com.rauldev.personalfinance.entry.web.obligation;

import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.CategorySummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code GET /api/v1/obligations/{obligationId}} and element of {@code GET /api/v1/obligations}.
 * {@code account} and {@code category} are the default payment source, and the status is the enum
 * constant name ({@code ACTIVE}, {@code ARCHIVED}).
 */
public record ObligationResponse(
    UUID id,
    String name,
    long amountCents,
    AccountSummaryResponse account,
    CategorySummaryResponse category,
    RecurrenceResponse recurrence,
    String status
) {

    public static ObligationResponse from(ObligationDetails obligation) {
        Objects.requireNonNull(obligation, "Obligation details cannot be null");
        return new ObligationResponse(
            obligation.id(),
            obligation.name(),
            MoneyCents.toCents(obligation.amount()),
            AccountSummaryResponse.from(obligation.account()),
            CategorySummaryResponse.from(obligation.category()),
            RecurrenceResponse.from(obligation.recurrence()),
            obligation.status().name());
    }
}
