package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.domain.Money;

class UpcomingCommitmentResponseTest {

    private static final ObligationSummary RENT =
        new ObligationSummary(UUID.fromString("40000000-0000-4000-8000-000000000001"), "Rent");
    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");

    @Test
    void mapsEveryFieldAndConvertsTheAmountToCents() {
        UpcomingCommitment commitment = new UpcomingCommitment(RENT, LocalDate.of(2026, 10, 15), Money.ofCents(2550),
            WALLET, false);

        UpcomingCommitmentResponse response = UpcomingCommitmentResponse.from(commitment);

        assertEquals(RENT.id(), response.obligation().id());
        assertEquals("Rent", response.obligation().name());
        assertEquals(LocalDate.of(2026, 10, 15), response.dueDate());
        assertEquals(2550, response.amountCents());
        assertEquals(WALLET.id(), response.account().id());
        assertEquals("Wallet", response.account().name());
        assertFalse(response.paymentBlocked());
    }

    @Test
    void keepsPaymentBlockedWhenTrue() {
        UpcomingCommitment commitment = new UpcomingCommitment(RENT, LocalDate.of(2026, 10, 15), Money.ofCents(100),
            WALLET, true);

        assertTrue(UpcomingCommitmentResponse.from(commitment).paymentBlocked());
    }

    @Test
    void rejectsNullCommitment() {
        assertThrows(NullPointerException.class, () -> UpcomingCommitmentResponse.from(null));
    }
}
