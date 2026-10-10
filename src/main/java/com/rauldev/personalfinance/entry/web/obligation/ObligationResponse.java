package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.application.readmodel.ObligationOverview;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.CategorySummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;

/**
 * Body of {@code GET /api/v1/obligations/{obligationId}} and element of {@code GET /api/v1/obligations}.
 * {@code account} and {@code category} are the default payment source, and the status is the enum
 * constant name ({@code ACTIVE}, {@code ARCHIVED}).
 *
 * <p>{@code nextDueDate} is the oldest unresolved occurrence (the oldest overdue date, otherwise the
 * next unresolved date on or after today), {@code null} when there is none or the obligation is
 * archived; {@code overdueCount} is 0 when archived. {@code archivedOn} is {@code null} for active
 * obligations and for obligations archived before the archive date was recorded.
 */
public record ObligationResponse(
    UUID id,
    String name,
    long amountCents,
    AccountSummaryResponse account,
    CategorySummaryResponse category,
    RecurrenceResponse recurrence,
    String status,
    LocalDate nextDueDate,
    long overdueCount,
    LocalDate archivedOn
) {

    public static ObligationResponse from(ObligationOverview overview) {
        Objects.requireNonNull(overview, "Obligation overview cannot be null");
        ObligationDetails obligation = overview.obligation();
        return new ObligationResponse(
            obligation.id(),
            obligation.name(),
            MoneyCents.toCents(obligation.amount()),
            AccountSummaryResponse.from(obligation.account()),
            CategorySummaryResponse.from(obligation.category()),
            RecurrenceResponse.from(obligation.recurrence()),
            obligation.status().name(),
            overview.nextDueDate(),
            overview.overdueCount(),
            obligation.archivedOn());
    }
}
