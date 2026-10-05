package com.rauldev.personalfinance.entry.web.operation;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.rauldev.personalfinance.application.usecase.RegisterTransferCommand;
import com.rauldev.personalfinance.domain.Money;

class RegisterTransferRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Test
    void mapsAllFieldsToCommand() {
        RegisterTransferCommand command = new RegisterTransferRequest(ACCOUNT_ID, OTHER_ACCOUNT_ID, 1005L, DATE).toCommand(USER_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals(ACCOUNT_ID, command.sourceAccountId());
        assertEquals(OTHER_ACCOUNT_ID, command.targetAccountId());
        assertEquals(Money.of("10.05"), command.amount());
        assertEquals(DATE, command.operationDate());
    }

    @Test
    void rejectsMissingSourceAccountIdNamingIt() {
        assertMissing("sourceAccountId", () -> new RegisterTransferRequest(null, OTHER_ACCOUNT_ID, 1L, DATE).toCommand(USER_ID));
    }

    @Test
    void rejectsMissingTargetAccountIdNamingIt() {
        assertMissing("targetAccountId", () -> new RegisterTransferRequest(ACCOUNT_ID, null, 1L, DATE).toCommand(USER_ID));
    }

    @Test
    void rejectsMissingAmountCentsNamingIt() {
        assertMissing("amountCents", () -> new RegisterTransferRequest(ACCOUNT_ID, OTHER_ACCOUNT_ID, null, DATE).toCommand(USER_ID));
    }

    @Test
    void rejectsMissingOperationDateNamingIt() {
        assertMissing("operationDate", () -> new RegisterTransferRequest(ACCOUNT_ID, OTHER_ACCOUNT_ID, 1L, null).toCommand(USER_ID));
    }

    @Test
    void rejectsNegativeAmountCents() {
        assertThrows(IllegalArgumentException.class,
            () -> new RegisterTransferRequest(ACCOUNT_ID, OTHER_ACCOUNT_ID, -1L, DATE).toCommand(USER_ID));
    }

    private static void assertMissing(String field, Executable executable) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);

        assertEquals("Field '" + field + "' is required", e.getMessage());
    }
}
