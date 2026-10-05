package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.CategorySummaryResponse;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.ObligationSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.TransferResponse;

/**
 * A recent active (not cancelled) operation on the dashboard.
 *
 * <p>Incomes and expenses carry {@code account} and {@code category}; transfers carry
 * {@code transfer}. {@code obligation} is the obligation whose occurrence the expense paid, and is
 * {@code null} otherwise. Absent parts are serialized as {@code null}, never omitted.
 */
public record RecentActivityItemResponse(
    UUID id,
    String type,
    long amountCents,
    LocalDate operationDate,
    AccountSummaryResponse account,
    CategorySummaryResponse category,
    TransferResponse transfer,
    ObligationSummaryResponse obligation
) {

    public static RecentActivityItemResponse from(RecentActivityItem item) {
        Objects.requireNonNull(item, "Recent activity item cannot be null");
        return new RecentActivityItemResponse(
            item.operationId(),
            item.operationType().name(),
            MoneyCents.toCents(item.amount()),
            item.operationDate(),
            account(item.account()),
            category(item.category()),
            transfer(item.transfer()),
            obligation(item.obligation()));
    }

    private static AccountSummaryResponse account(AccountSummary account) {
        return account == null ? null : AccountSummaryResponse.from(account);
    }

    private static CategorySummaryResponse category(CategorySummary category) {
        return category == null ? null : CategorySummaryResponse.from(category);
    }

    private static TransferResponse transfer(TransferDetails transfer) {
        return transfer == null ? null : TransferResponse.from(transfer);
    }

    private static ObligationSummaryResponse obligation(ObligationSummary obligation) {
        return obligation == null ? null : ObligationSummaryResponse.from(obligation);
    }
}
