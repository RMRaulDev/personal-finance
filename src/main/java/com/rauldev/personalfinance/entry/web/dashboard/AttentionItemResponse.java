package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.ObligationSummaryResponse;

/**
 * An element of the dashboard's {@code attention} list. Each kind has its own fields, and every
 * kind carries a {@code type} field ({@code OVERDUE_OCCURRENCE}, {@code SHORTFALL},
 * {@code PAYMENT_BLOCKED}, {@code ACCOUNT_SHORTFALL}) that tells the client which one it is.
 *
 * <p>How it serializes: the list is declared as {@code List<AttentionItemResponse>}, but Jackson
 * picks the serializer from each element's <em>runtime</em> class, so every element is written
 * with the components of its own record. {@code type} is an ordinary record component, so it is
 * written like any other field.
 *
 * <p>Why no {@code @JsonTypeInfo}/{@code @JsonSubTypes}: those annotations exist so that Jackson
 * can <em>read</em> polymorphic JSON back into the right subtype. This API only writes these
 * items, so an explicit {@code type} component is enough, keeps the contract visible in the
 * record, and does not tie the JSON shape to Jackson-specific annotations.
 *
 * <p>{@link #from(AttentionItem)} switches over the sealed {@link AttentionItem} without a
 * {@code default} branch: adding a new kind to the Core breaks compilation here until it is
 * mapped.
 */
public sealed interface AttentionItemResponse {

    String type();

    static AttentionItemResponse from(AttentionItem item) {
        Objects.requireNonNull(item, "Attention item cannot be null");
        String type = item.type().name();
        return switch (item) {
            case AttentionItem.OverdueOccurrence overdue -> new OverdueOccurrenceResponse(
                type,
                ObligationSummaryResponse.from(overdue.obligation()),
                overdue.oldestOverdue(),
                overdue.overdueCount(),
                MoneyCents.toCents(overdue.overdueAmount()));
            case AttentionItem.Shortfall shortfall -> new ShortfallResponse(
                type,
                MoneyCents.toCents(shortfall.shortfall()));
            case AttentionItem.PaymentBlocked blocked -> new PaymentBlockedResponse(
                type,
                ObligationSummaryResponse.from(blocked.obligation()),
                AccountSummaryResponse.from(blocked.account()),
                blocked.nearestDueDate(),
                MoneyCents.toCents(blocked.committed()),
                blocked.accountInactive(),
                blocked.categoryInactive());
            case AttentionItem.AccountShortfall accountShortfall -> new AccountShortfallResponse(
                type,
                AccountSummaryResponse.from(accountShortfall.account()),
                MoneyCents.toCents(accountShortfall.shortfall()),
                accountShortfall.nearestDueDate());
        };
    }

    /**
     * {@code OVERDUE_OCCURRENCE}: an obligation has unresolved occurrences due before today.
     */
    record OverdueOccurrenceResponse(
        String type,
        ObligationSummaryResponse obligation,
        LocalDate oldestOverdue,
        long overdueCount,
        long overdueAmountCents
    ) implements AttentionItemResponse {
    }

    /**
     * {@code SHORTFALL}: the active accounts do not cover the committed amount.
     */
    record ShortfallResponse(String type, long shortfallCents) implements AttentionItemResponse {
    }

    /**
     * {@code PAYMENT_BLOCKED}: an obligation with a commitment (overdue or within the horizon)
     * cannot be paid because its account or category is inactive.
     */
    record PaymentBlockedResponse(
        String type,
        ObligationSummaryResponse obligation,
        AccountSummaryResponse account,
        LocalDate nearestDueDate,
        long committedCents,
        boolean accountInactive,
        boolean categoryInactive
    ) implements AttentionItemResponse {
    }

    /**
     * {@code ACCOUNT_SHORTFALL}: one account does not cover the commitments charged to it.
     */
    record AccountShortfallResponse(
        String type,
        AccountSummaryResponse account,
        long shortfallCents,
        LocalDate nearestDueDate
    ) implements AttentionItemResponse {
    }
}
