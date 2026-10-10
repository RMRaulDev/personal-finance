package com.rauldev.personalfinance.entry.web.setup;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.CompleteInitialSetupCommand;
import com.rauldev.personalfinance.entry.web.common.MoneyCents;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/setup}. Both fields are required; {@code openingBalanceCents} may be
 * {@code 0}.
 */
public record CompleteInitialSetupRequest(String accountName, Long openingBalanceCents) {

    /**
     * @throws IllegalArgumentException if a field is missing or {@code openingBalanceCents} is negative
     */
    public CompleteInitialSetupCommand toCommand(UUID userId) {
        return new CompleteInitialSetupCommand(
            userId,
            RequiredFields.require(accountName, "accountName"),
            MoneyCents.toMoney(openingBalanceCents, "openingBalanceCents"));
    }
}
