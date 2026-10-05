package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.rauldev.personalfinance.application.usecase.CreateObligationCommand;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;

class CreateObligationRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final RecurrenceRequest RECURRENCE =
        new RecurrenceRequest(Frequency.MONTHLY, LocalDate.of(2026, 10, 15), null);

    @Test
    void mapsAllFieldsToCommand() {
        CreateObligationCommand command =
            new CreateObligationRequest("Rent", 1005L, ACCOUNT_ID, CATEGORY_ID, RECURRENCE).toCommand(USER_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals(ACCOUNT_ID, command.accountId());
        assertEquals(CATEGORY_ID, command.categoryId());
        assertEquals("Rent", command.name());
        assertEquals(Money.of("10.05"), command.amount());
        assertEquals(Frequency.MONTHLY, command.recurrence().frequency());
        assertEquals(LocalDate.of(2026, 10, 15), command.recurrence().startDate());
    }

    @Test
    void rejectsMissingNameNamingIt() {
        assertMissing("name", () -> new CreateObligationRequest(null, 1L, ACCOUNT_ID, CATEGORY_ID, RECURRENCE)
            .toCommand(USER_ID));
    }

    @Test
    void rejectsMissingAmountCentsNamingIt() {
        assertMissing("amountCents", () -> new CreateObligationRequest("Rent", null, ACCOUNT_ID, CATEGORY_ID,
            RECURRENCE).toCommand(USER_ID));
    }

    @Test
    void rejectsMissingAccountIdNamingIt() {
        assertMissing("accountId", () -> new CreateObligationRequest("Rent", 1L, null, CATEGORY_ID, RECURRENCE)
            .toCommand(USER_ID));
    }

    @Test
    void rejectsMissingCategoryIdNamingIt() {
        assertMissing("categoryId", () -> new CreateObligationRequest("Rent", 1L, ACCOUNT_ID, null, RECURRENCE)
            .toCommand(USER_ID));
    }

    @Test
    void rejectsMissingRecurrenceNamingIt() {
        assertMissing("recurrence", () -> new CreateObligationRequest("Rent", 1L, ACCOUNT_ID, CATEGORY_ID, null)
            .toCommand(USER_ID));
    }

    @Test
    void rejectsMissingRecurrenceFrequencyNamingTheDottedField() {
        assertMissing("recurrence.frequency", () -> new CreateObligationRequest("Rent", 1L, ACCOUNT_ID,
            CATEGORY_ID, new RecurrenceRequest(null, LocalDate.of(2026, 10, 15), null)).toCommand(USER_ID));
    }

    @Test
    void rejectsNegativeAmountCents() {
        assertThrows(IllegalArgumentException.class,
            () -> new CreateObligationRequest("Rent", -1L, ACCOUNT_ID, CATEGORY_ID, RECURRENCE).toCommand(USER_ID));
    }

    private static void assertMissing(String field, Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Field '" + field + "' is required", e.getMessage());
    }
}
