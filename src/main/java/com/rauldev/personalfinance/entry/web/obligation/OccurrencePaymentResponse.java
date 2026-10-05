package com.rauldev.personalfinance.entry.web.obligation;

import java.util.UUID;

/**
 * Body of a paid occurrence ({@code 201 Created}): the id of the expense registered as the
 * payment. It is an operation, not an obligation resource, hence {@code operationId} instead of
 * {@code id}.
 */
public record OccurrencePaymentResponse(UUID operationId) {
}
