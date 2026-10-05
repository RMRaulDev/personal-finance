package com.rauldev.personalfinance.entry.web.common;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;

class AccountSummaryResponseTest {

    private static final UUID ID = UUID.fromString("123e4567-e89b-42d3-a456-426614174000");

    @Test
    void mapsIdAndName() {
        AccountSummaryResponse response = AccountSummaryResponse.from(new AccountSummary(ID, "Checking"));

        assertEquals(new AccountSummaryResponse(ID, "Checking"), response);
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> AccountSummaryResponse.from(null));
    }
}
