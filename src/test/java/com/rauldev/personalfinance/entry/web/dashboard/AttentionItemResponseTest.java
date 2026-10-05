package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.domain.AttentionType;
import com.rauldev.personalfinance.domain.Money;

class AttentionItemResponseTest {

    private static final ObligationSummary RENT =
        new ObligationSummary(UUID.fromString("40000000-0000-4000-8000-000000000001"), "Rent");
    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");

    @Test
    void mapsOverdueOccurrenceWithItsTypeAndCents() {
        AttentionItem item = new AttentionItem.OverdueOccurrence(RENT, LocalDate.of(2026, 9, 20), 2,
            Money.ofCents(5000));

        AttentionItemResponse response = AttentionItemResponse.from(item);

        AttentionItemResponse.OverdueOccurrenceResponse overdue =
            assertInstanceOf(AttentionItemResponse.OverdueOccurrenceResponse.class, response);
        assertEquals(AttentionType.OVERDUE_OCCURRENCE.name(), overdue.type());
        assertEquals(RENT.id(), overdue.obligation().id());
        assertEquals("Rent", overdue.obligation().name());
        assertEquals(LocalDate.of(2026, 9, 20), overdue.oldestOverdue());
        assertEquals(2, overdue.overdueCount());
        assertEquals(5000, overdue.overdueAmountCents());
    }

    @Test
    void mapsShortfallWithItsTypeAndCents() {
        AttentionItemResponse response = AttentionItemResponse.from(new AttentionItem.Shortfall(Money.ofCents(3500)));

        AttentionItemResponse.ShortfallResponse shortfall =
            assertInstanceOf(AttentionItemResponse.ShortfallResponse.class, response);
        assertEquals(AttentionType.SHORTFALL.name(), shortfall.type());
        assertEquals(3500, shortfall.shortfallCents());
    }

    @Test
    void mapsPaymentBlockedWithAnInactiveAccount() {
        AttentionItem item = new AttentionItem.PaymentBlocked(RENT, WALLET, LocalDate.of(2026, 10, 10),
            Money.ofCents(1000), true, false);

        AttentionItemResponse response = AttentionItemResponse.from(item);

        AttentionItemResponse.PaymentBlockedResponse blocked =
            assertInstanceOf(AttentionItemResponse.PaymentBlockedResponse.class, response);
        assertEquals(AttentionType.PAYMENT_BLOCKED.name(), blocked.type());
        assertEquals(RENT.id(), blocked.obligation().id());
        assertEquals(WALLET.id(), blocked.account().id());
        assertEquals("Wallet", blocked.account().name());
        assertEquals(LocalDate.of(2026, 10, 10), blocked.nearestDueDate());
        assertEquals(1000, blocked.committedCents());
        assertTrue(blocked.accountInactive());
        assertFalse(blocked.categoryInactive());
    }

    @Test
    void mapsPaymentBlockedWithAnInactiveCategory() {
        AttentionItem item = new AttentionItem.PaymentBlocked(RENT, WALLET, LocalDate.of(2026, 10, 10),
            Money.ofCents(1000), false, true);

        AttentionItemResponse.PaymentBlockedResponse blocked =
            assertInstanceOf(AttentionItemResponse.PaymentBlockedResponse.class, AttentionItemResponse.from(item));

        assertFalse(blocked.accountInactive());
        assertTrue(blocked.categoryInactive());
    }

    @Test
    void mapsAccountShortfallWithItsTypeAndCents() {
        AttentionItem item = new AttentionItem.AccountShortfall(WALLET, Money.ofCents(1500), LocalDate.of(2026, 10, 12));

        AttentionItemResponse response = AttentionItemResponse.from(item);

        AttentionItemResponse.AccountShortfallResponse shortfall =
            assertInstanceOf(AttentionItemResponse.AccountShortfallResponse.class, response);
        assertEquals(AttentionType.ACCOUNT_SHORTFALL.name(), shortfall.type());
        assertEquals(WALLET.id(), shortfall.account().id());
        assertEquals(1500, shortfall.shortfallCents());
        assertEquals(LocalDate.of(2026, 10, 12), shortfall.nearestDueDate());
    }

    @Test
    void rejectsNullAttentionItem() {
        assertThrows(NullPointerException.class, () -> AttentionItemResponse.from(null));
    }
}
