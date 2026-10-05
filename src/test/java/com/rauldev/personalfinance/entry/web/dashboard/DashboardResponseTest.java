package com.rauldev.personalfinance.entry.web.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.AttentionItem;
import com.rauldev.personalfinance.application.readmodel.AvailableToSpend;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.Dashboard;
import com.rauldev.personalfinance.application.readmodel.DashboardHorizon;
import com.rauldev.personalfinance.application.readmodel.ObligationSummary;
import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.application.readmodel.UpcomingCommitment;
import com.rauldev.personalfinance.domain.Money;

class DashboardResponseTest {

    private static final DashboardHorizon HORIZON =
        new DashboardHorizon(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 18));
    private static final AvailableToSpend AVAILABLE =
        new AvailableToSpend(Money.ofCents(2500), Money.ofCents(6000), Money.ofCents(0), Money.ofCents(3500));
    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");
    private static final ObligationSummary RENT =
        new ObligationSummary(UUID.fromString("40000000-0000-4000-8000-000000000001"), "Rent");

    private static DashboardResponse emptyLists() {
        return new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON),
            DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            List.of(), List.of(), List.of());
    }

    @Test
    void mapsTheHorizonInclusiveBounds() {
        DashboardResponse.HorizonResponse response = DashboardResponse.HorizonResponse.from(HORIZON);

        assertEquals(LocalDate.of(2026, 10, 5), response.from());
        assertEquals(LocalDate.of(2026, 10, 18), response.to());
    }

    @Test
    void mapsAvailableToSpendToCents() {
        DashboardResponse.AvailableToSpendResponse response = DashboardResponse.AvailableToSpendResponse.from(AVAILABLE);

        assertEquals(2500, response.balanceCents());
        assertEquals(6000, response.committedCents());
        assertEquals(0, response.availableCents());
        assertEquals(3500, response.shortfallCents());
    }

    @Test
    void mapsAllSectionsOfTheDashboard() {
        Dashboard dashboard = new Dashboard(HORIZON, AVAILABLE,
            List.of(new AttentionItem.Shortfall(Money.ofCents(3500))),
            List.of(new UpcomingCommitment(RENT, LocalDate.of(2026, 10, 15), Money.ofCents(3000), WALLET, false)),
            List.of(new RecentActivityItem(UUID.fromString("50000000-0000-4000-8000-000000000001"),
                OperationType.INCOME, Money.ofCents(100), LocalDate.of(2026, 10, 4), WALLET,
                new CategorySummary(UUID.fromString("30000000-0000-4000-8000-000000000001"), "Salary"), null, null)));

        DashboardResponse response = DashboardResponse.from(dashboard);

        assertEquals(LocalDate.of(2026, 10, 5), response.horizon().from());
        assertEquals(3500, response.availableToSpend().shortfallCents());
        assertEquals(1, response.attention().size());
        assertEquals(1, response.upcomingCommitments().size());
        assertEquals(3000, response.upcomingCommitments().get(0).amountCents());
        assertEquals(1, response.recent().size());
        assertEquals("INCOME", response.recent().get(0).type());
    }

    @Test
    void preservesTheAttentionOrderOfTheCore() {
        Dashboard dashboard = new Dashboard(HORIZON, AVAILABLE,
            List.of(
                new AttentionItem.OverdueOccurrence(RENT, LocalDate.of(2026, 9, 20), 1, Money.ofCents(3000)),
                new AttentionItem.Shortfall(Money.ofCents(3500)),
                new AttentionItem.PaymentBlocked(RENT, WALLET, LocalDate.of(2026, 10, 10), Money.ofCents(1000), true,
                    false),
                new AttentionItem.AccountShortfall(WALLET, Money.ofCents(1000), LocalDate.of(2026, 9, 20))),
            List.of(), List.of());

        DashboardResponse response = DashboardResponse.from(dashboard);

        assertEquals(List.of("OVERDUE_OCCURRENCE", "SHORTFALL", "PAYMENT_BLOCKED", "ACCOUNT_SHORTFALL"),
            response.attention().stream().map(AttentionItemResponse::type).toList());
        assertInstanceOf(AttentionItemResponse.OverdueOccurrenceResponse.class, response.attention().get(0));
        assertInstanceOf(AttentionItemResponse.AccountShortfallResponse.class, response.attention().get(3));
    }

    @Test
    void mapsEmptyListsToEmptyLists() {
        Dashboard dashboard = new Dashboard(HORIZON, AVAILABLE, List.of(), List.of(), List.of());

        DashboardResponse response = DashboardResponse.from(dashboard);

        assertEquals(List.of(), response.attention());
        assertEquals(List.of(), response.upcomingCommitments());
        assertEquals(List.of(), response.recent());
    }

    @Test
    void copiesTheListsDefensively() {
        List<AttentionItemResponse> attention = new ArrayList<>();
        attention.add(new AttentionItemResponse.ShortfallResponse("SHORTFALL", 1));
        DashboardResponse response = new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON),
            DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            attention, List.of(), List.of());

        attention.clear();

        assertEquals(1, response.attention().size());
    }

    @Test
    void exposesUnmodifiableLists() {
        DashboardResponse response = emptyLists();

        assertThrows(UnsupportedOperationException.class, () -> response.attention().add(null));
        assertThrows(UnsupportedOperationException.class, () -> response.upcomingCommitments().add(null));
        assertThrows(UnsupportedOperationException.class, () -> response.recent().add(null));
    }

    @Test
    void rejectsNullAttention() {
        assertThrows(NullPointerException.class, () -> new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON), DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            null, List.of(), List.of()));
    }

    @Test
    void rejectsNullUpcomingCommitments() {
        assertThrows(NullPointerException.class, () -> new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON), DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            List.of(), null, List.of()));
    }

    @Test
    void rejectsNullRecent() {
        assertThrows(NullPointerException.class, () -> new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON), DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            List.of(), List.of(), null));
    }

    @Test
    void rejectsListContainingNullItem() {
        List<AttentionItemResponse> attention = new ArrayList<>();
        attention.add(null);

        assertThrows(NullPointerException.class, () -> new DashboardResponse(
            DashboardResponse.HorizonResponse.from(HORIZON), DashboardResponse.AvailableToSpendResponse.from(AVAILABLE),
            attention, List.of(), List.of()));
    }

    @Test
    void rejectsNullDashboard() {
        assertThrows(NullPointerException.class, () -> DashboardResponse.from(null));
    }

    @Test
    void rejectsNullHorizon() {
        assertThrows(NullPointerException.class, () -> DashboardResponse.HorizonResponse.from(null));
    }

    @Test
    void rejectsNullAvailableToSpend() {
        assertThrows(NullPointerException.class, () -> DashboardResponse.AvailableToSpendResponse.from(null));
    }
}
