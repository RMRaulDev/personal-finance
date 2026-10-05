package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.usecase.ModifyObligationCommand;
import com.rauldev.personalfinance.domain.Frequency;
import com.rauldev.personalfinance.domain.Money;

class ModifyObligationRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OBLIGATION_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Test
    void absentFieldsStayNullInTheCommand() {
        ModifyObligationCommand command =
            new ModifyObligationRequest("Rent", null, null, null, null).toCommand(USER_ID, OBLIGATION_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals(OBLIGATION_ID, command.obligationId());
        assertEquals("Rent", command.name());
        assertNull(command.amount());
        assertNull(command.accountId());
        assertNull(command.categoryId());
        assertNull(command.recurrence());
    }

    @Test
    void mapsAmountCentsToMoney() {
        ModifyObligationCommand command =
            new ModifyObligationRequest(null, 1005L, null, null, null).toCommand(USER_ID, OBLIGATION_ID);

        assertEquals(Money.of("10.05"), command.amount());
    }

    @Test
    void mapsAccountAndCategoryIds() {
        ModifyObligationCommand command = new ModifyObligationRequest(null, null, ACCOUNT_ID, CATEGORY_ID, null)
            .toCommand(USER_ID, OBLIGATION_ID);

        assertEquals(ACCOUNT_ID, command.accountId());
        assertEquals(CATEGORY_ID, command.categoryId());
    }

    @Test
    void mapsRecurrenceAndAnAbsentEndDateStaysEmpty() {
        ModifyObligationCommand command = new ModifyObligationRequest(null, null, null, null,
            new RecurrenceRequest(Frequency.WEEKLY, LocalDate.of(2026, 10, 10), null))
            .toCommand(USER_ID, OBLIGATION_ID);

        assertEquals(Frequency.WEEKLY, command.recurrence().frequency());
        assertEquals(LocalDate.of(2026, 10, 10), command.recurrence().startDate());
        assertTrue(command.recurrence().endDate().isEmpty());
    }

    @Test
    void rejectsIncompleteRecurrenceNamingTheDottedField() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ModifyObligationRequest(null, null, null, null,
                new RecurrenceRequest(Frequency.WEEKLY, null, null)).toCommand(USER_ID, OBLIGATION_ID));

        assertEquals("Field 'recurrence.startDate' is required", e.getMessage());
    }

    @Test
    void rejectsARequestWithoutAnyChange() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ModifyObligationRequest(null, null, null, null, null).toCommand(USER_ID, OBLIGATION_ID));

        assertEquals("At least one change is required", e.getMessage());
    }

    @Test
    void rejectsNegativeAmountCents() {
        assertThrows(IllegalArgumentException.class,
            () -> new ModifyObligationRequest(null, -1L, null, null, null).toCommand(USER_ID, OBLIGATION_ID));
    }
}
