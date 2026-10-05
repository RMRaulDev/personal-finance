package com.rauldev.personalfinance.entry.web.operation;

import java.util.List;
import java.util.Objects;

/**
 * Body of {@code GET /api/v1/operations}: one page of the history, plus the page number and size
 * that were applied (the defaults when the client sent none).
 *
 * <p>There is no total count or {@code hasNext} flag (the Core query does not provide them): the
 * client has reached the last page when {@code items} has fewer than {@code pageSize} elements. A
 * page beyond the end answers an empty {@code items} list.
 */
public record OperationPageResponse(List<OperationResponse> items, int page, int pageSize) {

    public OperationPageResponse {
        items = List.copyOf(Objects.requireNonNull(items, "Items cannot be null"));
    }
}
