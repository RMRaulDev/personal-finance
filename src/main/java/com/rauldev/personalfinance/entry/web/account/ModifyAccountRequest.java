package com.rauldev.personalfinance.entry.web.account;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.ModifyAccountCommand;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code PATCH /api/v1/accounts/{accountId}}. Renaming is the only supported change, so
 * {@code name} is required.
 */
public record ModifyAccountRequest(String name) {

    /**
     * @throws IllegalArgumentException if {@code name} is missing
     */
    public ModifyAccountCommand toCommand(UUID userId, UUID accountId) {
        return new ModifyAccountCommand(userId, accountId, RequiredFields.require(name, "name"));
    }
}
