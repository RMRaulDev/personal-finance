package com.rauldev.personalfinance.application.readmodel;

import java.util.UUID;

public record AccountSummary(
    UUID id,
    String name
) {
}
