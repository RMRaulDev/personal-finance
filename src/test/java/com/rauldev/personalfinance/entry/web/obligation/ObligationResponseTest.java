package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.ObligationDetails;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.ObligationStatus;
import com.rauldev.personalfinance.domain.Recurrence;
import com.rauldev.personalfinance.entry.web.common.AccountSummaryResponse;
import com.rauldev.personalfinance.entry.web.common.CategorySummaryResponse;

class ObligationResponseTest {

    private static final UUID ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Test
    void mapsEveryFieldWithEndDate() {
        ObligationDetails details = new ObligationDetails(ID, "Rent", Money.ofCents(2500),
            new AccountSummary(ACCOUNT_ID, "Wallet"), new CategorySummary(CATEGORY_ID, "Housing"),
            new Recurrence(Frequency.MONTHLY, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 12, 20)),
            ObligationStatus.ACTIVE);

        assertEquals(new ObligationResponse(ID, "Rent", 2500,
            new AccountSummaryResponse(ACCOUNT_ID, "Wallet"), new CategorySummaryResponse(CATEGORY_ID, "Housing"),
            new RecurrenceResponse("MONTHLY", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 12, 20)), "ACTIVE"),
            ObligationResponse.from(details));
    }

    @Test
    void mapsArchivedStatusAndNullEndDate() {
        ObligationDetails details = new ObligationDetails(ID, "Rent", Money.ofCents(1),
            new AccountSummary(ACCOUNT_ID, "Wallet"), new CategorySummary(CATEGORY_ID, "Housing"),
            new Recurrence(Frequency.YEARLY, LocalDate.of(2026, 10, 20), null), ObligationStatus.ARCHIVED);

        ObligationResponse response = ObligationResponse.from(details);

        assertEquals("ARCHIVED", response.status());
        assertEquals(1, response.amountCents());
        assertNull(response.recurrence().endDate());
    }

    @Test
    void rejectsNullDetails() {
        assertThrows(NullPointerException.class, () -> ObligationResponse.from(null));
    }
}
