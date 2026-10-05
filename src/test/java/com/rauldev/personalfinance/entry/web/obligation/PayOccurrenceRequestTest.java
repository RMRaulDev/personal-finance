package com.rauldev.personalfinance.entry.web.obligation;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.usecase.PayOccurrenceCommand;
import com.rauldev.personalfinance.domain.Money;

class PayOccurrenceRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OBLIGATION_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 15);

    @Test
    void everyFieldIsOptional() {
        PayOccurrenceCommand command =
            new PayOccurrenceRequest(null, null, null, null).toCommand(USER_ID, OBLIGATION_ID, DUE_DATE);

        assertNull(command.amount());
        assertNull(command.operationDate());
        assertNull(command.accountId());
        assertNull(command.categoryId());
    }

    @Test
    void passesPathValuesAndCurrentUserToTheCommand() {
        PayOccurrenceCommand command =
            new PayOccurrenceRequest(null, null, null, null).toCommand(USER_ID, OBLIGATION_ID, DUE_DATE);

        assertEquals(USER_ID, command.userId());
        assertEquals(OBLIGATION_ID, command.obligationId());
        assertEquals(DUE_DATE, command.dueDate());
    }

    @Test
    void mapsAmountCentsToMoney() {
        PayOccurrenceCommand command =
            new PayOccurrenceRequest(1005L, null, null, null).toCommand(USER_ID, OBLIGATION_ID, DUE_DATE);

        assertEquals(Money.of("10.05"), command.amount());
    }

    @Test
    void passesOverridesThrough() {
        LocalDate operationDate = LocalDate.of(2026, 10, 3);

        PayOccurrenceCommand command = new PayOccurrenceRequest(null, operationDate, ACCOUNT_ID, CATEGORY_ID)
            .toCommand(USER_ID, OBLIGATION_ID, DUE_DATE);

        assertEquals(operationDate, command.operationDate());
        assertEquals(ACCOUNT_ID, command.accountId());
        assertEquals(CATEGORY_ID, command.categoryId());
    }

    @Test
    void rejectsNegativeAmountCents() {
        assertThrows(IllegalArgumentException.class,
            () -> new PayOccurrenceRequest(-1L, null, null, null).toCommand(USER_ID, OBLIGATION_ID, DUE_DATE));
    }

    @Test
    void rejectsZeroAmountCents() {
        assertThrows(IllegalArgumentException.class,
            () -> new PayOccurrenceRequest(0L, null, null, null).toCommand(USER_ID, OBLIGATION_ID, DUE_DATE));
    }
}
