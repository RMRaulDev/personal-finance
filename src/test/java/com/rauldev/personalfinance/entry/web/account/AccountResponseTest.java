package com.rauldev.personalfinance.entry.web.account;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.readmodel.AccountDetails;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

class AccountResponseTest {

    private static final UUID ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    void mapsBalanceToCentsAndStatusToName() {
        AccountDetails details = new AccountDetails(ID, USER_ID, "Wallet", Money.ofCents(123456), AccountStatus.INACTIVE);

        AccountResponse response = AccountResponse.from(details);

        assertEquals(new AccountResponse(ID, "Wallet", 123456L, "INACTIVE"), response);
    }

    @Test
    void mapsZeroBalanceToZeroCents() {
        AccountDetails details = new AccountDetails(ID, USER_ID, "Wallet", Money.ofCents(0), AccountStatus.ACTIVE);

        assertEquals(0L, AccountResponse.from(details).balanceCents());
    }

    @Test
    void rejectsNullDetails() {
        assertThrows(NullPointerException.class, () -> AccountResponse.from(null));
    }
}
