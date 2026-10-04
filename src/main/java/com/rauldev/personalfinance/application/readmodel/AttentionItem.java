package com.rauldev.personalfinance.application.readmodel;

import java.time.LocalDate;
import java.util.Objects;

import com.rauldev.personalfinance.domain.AttentionType;
import com.rauldev.personalfinance.domain.Money;

/**
 * An item that needs the user's attention on the dashboard, with the names the client displays.
 */
public sealed interface AttentionItem {

    AttentionType type();

    record OverdueOccurrence(
        ObligationSummary obligation,
        LocalDate oldestOverdue,
        long overdueCount,
        Money overdueAmount
    ) implements AttentionItem {

        public OverdueOccurrence {
            Objects.requireNonNull(obligation, "Obligation cannot be null");
            Objects.requireNonNull(oldestOverdue, "Oldest overdue cannot be null");
            if (overdueCount <= 0) {
                throw new IllegalArgumentException("Overdue count must be greater than zero");
            }
            Objects.requireNonNull(overdueAmount, "Overdue amount cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.OVERDUE_OCCURRENCE;
        }
    }

    record Shortfall(Money shortfall) implements AttentionItem {

        public Shortfall {
            Objects.requireNonNull(shortfall, "Shortfall cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.SHORTFALL;
        }
    }

    record PaymentBlocked(
        ObligationSummary obligation,
        AccountSummary account,
        LocalDate nearestDueDate,
        Money committed,
        boolean accountInactive,
        boolean categoryInactive
    ) implements AttentionItem {

        public PaymentBlocked {
            Objects.requireNonNull(obligation, "Obligation cannot be null");
            Objects.requireNonNull(account, "Account cannot be null");
            Objects.requireNonNull(nearestDueDate, "Nearest due date cannot be null");
            Objects.requireNonNull(committed, "Committed amount cannot be null");
            if (!accountInactive && !categoryInactive) {
                throw new IllegalArgumentException("A blocked payment requires an inactive account or category");
            }
        }

        @Override
        public AttentionType type() {
            return AttentionType.PAYMENT_BLOCKED;
        }
    }

    record AccountShortfall(
        AccountSummary account,
        Money shortfall,
        LocalDate nearestDueDate
    ) implements AttentionItem {

        public AccountShortfall {
            Objects.requireNonNull(account, "Account cannot be null");
            Objects.requireNonNull(shortfall, "Shortfall cannot be null");
            Objects.requireNonNull(nearestDueDate, "Nearest due date cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.ACCOUNT_SHORTFALL;
        }
    }
}
