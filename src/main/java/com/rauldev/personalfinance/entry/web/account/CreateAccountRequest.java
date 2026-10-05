package com.rauldev.personalfinance.entry.web.account;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.CreateAccountCommand;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/accounts}.
 */
public record CreateAccountRequest(String name) {

    /**
     * @throws IllegalArgumentException if {@code name} is missing
     */
    public CreateAccountCommand toCommand(UUID userId) {
        return new CreateAccountCommand(userId, RequiredFields.require(name, "name"));
    }
}
