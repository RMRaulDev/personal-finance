package com.rauldev.personalfinance.entry.web.account;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.usecase.CreateAccountCommand;

class CreateAccountRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    void passesNameAndUserToCommand() {
        CreateAccountCommand command = new CreateAccountRequest("Wallet").toCommand(USER_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals("Wallet", command.name());
    }

    @Test
    void rejectsMissingName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new CreateAccountRequest(null).toCommand(USER_ID));

        assertEquals("Field 'name' is required", e.getMessage());
    }
}
