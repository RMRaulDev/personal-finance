package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CommitmentSummaryTest {
    private static final Money ZERO = Money.ofCents(0);

    private static CommitmentSummary summary(List<ObligationCommitment> obligations, List<AccountCommitment> accounts,
                                             List<Attention> attention) {
        return new CommitmentSummary(ZERO, ZERO, ZERO, ZERO, obligations, accounts, attention);
    }

    @Test
    void rejectsNullFields() {
        assertThrows(NullPointerException.class,
            () -> new CommitmentSummary(null, ZERO, ZERO, ZERO, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new CommitmentSummary(ZERO, null, ZERO, ZERO, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new CommitmentSummary(ZERO, ZERO, null, ZERO, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
            () -> new CommitmentSummary(ZERO, ZERO, ZERO, null, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> summary(null, List.of(), List.of()));
        assertThrows(NullPointerException.class, () -> summary(List.of(), null, List.of()));
        assertThrows(NullPointerException.class, () -> summary(List.of(), List.of(), null));
    }

    @Test
    void copiesListsDefensively() {
        List<Attention> attention = new ArrayList<>();
        attention.add(new Attention.Shortfall(Money.ofCents(100)));

        CommitmentSummary summary = summary(List.of(), List.of(), attention);
        attention.clear();

        assertEquals(1, summary.attention().size());
        assertThrows(UnsupportedOperationException.class,
            () -> summary.attention().add(new Attention.Shortfall(Money.ofCents(1))));
        assertThrows(UnsupportedOperationException.class,
            () -> summary.obligations().add(new ObligationCommitment(UUID.randomUUID(), ZERO, 0, ZERO, List.of(),
                false)));
        assertThrows(UnsupportedOperationException.class,
            () -> summary.accounts().add(new AccountCommitment(UUID.randomUUID(), ZERO, ZERO, ZERO)));
    }
}
