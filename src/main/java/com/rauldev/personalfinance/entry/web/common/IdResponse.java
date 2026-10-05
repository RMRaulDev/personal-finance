package com.rauldev.personalfinance.entry.web.common;

import java.util.UUID;

/**
 * Body of a {@code 201 Created} response: the id of the created resource.
 */
public record IdResponse(UUID id) {
}
