package com.rauldev.personalfinance.application.readmodel;

import java.util.UUID;

public record CategorySummary(
    UUID id,
    String name
) {
}
