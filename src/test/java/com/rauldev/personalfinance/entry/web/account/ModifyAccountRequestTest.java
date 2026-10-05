package com.rauldev.personalfinance.entry.web.account;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.usecase.ModifyAccountCommand;

class ModifyAccountRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");

    @Test
    void passesNameUserAndAccountToCommand() {
        ModifyAccountCommand command = new ModifyAccountRequest("Savings").toCommand(USER_ID, ACCOUNT_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals(ACCOUNT_ID, command.accountId());
        assertEquals("Savings", command.name());
    }

    @Test
    void rejectsMissingName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new ModifyAccountRequest(null).toCommand(USER_ID, ACCOUNT_ID));

        assertEquals("Field 'name' is required", e.getMessage());
    }
}
