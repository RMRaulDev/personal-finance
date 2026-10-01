package com.rauldev.personalfinance.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * An item that needs the user's attention, produced by {@link CommitmentCalculator}.
 */
public sealed interface Attention {

    AttentionType type();

    record OverdueOccurrence(UUID obligationId, LocalDate oldestOverdue, long overdueCount, Money overdueAmount)
        implements Attention {

        public OverdueOccurrence {
            Objects.requireNonNull(obligationId, "Obligation id cannot be null");
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

    record Shortfall(Money shortfall) implements Attention {

        public Shortfall {
            Objects.requireNonNull(shortfall, "Shortfall cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.SHORTFALL;
        }
    }

    record PaymentBlocked(UUID obligationId, LocalDate nearestDueDate, Money committed) implements Attention {

        public PaymentBlocked {
            Objects.requireNonNull(obligationId, "Obligation id cannot be null");
            Objects.requireNonNull(nearestDueDate, "Nearest due date cannot be null");
            Objects.requireNonNull(committed, "Committed amount cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.PAYMENT_BLOCKED;
        }
    }

    record AccountShortfall(UUID accountId, Money shortfall, LocalDate nearestDueDate) implements Attention {

        public AccountShortfall {
            Objects.requireNonNull(accountId, "Account id cannot be null");
            Objects.requireNonNull(shortfall, "Shortfall cannot be null");
            Objects.requireNonNull(nearestDueDate, "Nearest due date cannot be null");
        }

        @Override
        public AttentionType type() {
            return AttentionType.ACCOUNT_SHORTFALL;
        }
    }
}
