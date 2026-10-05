package com.rauldev.personalfinance.entry.web.operation;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;

class TransferResponseTest {

    private static final AccountSummary WALLET =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000001"), "Wallet");
    private static final AccountSummary SAVINGS =
        new AccountSummary(UUID.fromString("10000000-0000-4000-8000-000000000002"), "Savings");

    @Test
    void mapsSourceAndTargetAccounts() {
        TransferResponse response = TransferResponse.from(new TransferDetails(WALLET, SAVINGS));

        assertEquals(WALLET.id(), response.sourceAccount().id());
        assertEquals("Wallet", response.sourceAccount().name());
        assertEquals(SAVINGS.id(), response.targetAccount().id());
        assertEquals("Savings", response.targetAccount().name());
    }

    @Test
    void rejectsNullTransferDetails() {
        assertThrows(NullPointerException.class, () -> TransferResponse.from(null));
    }
}
