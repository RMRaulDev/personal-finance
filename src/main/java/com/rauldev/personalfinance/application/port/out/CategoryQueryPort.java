package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.CategoryDetails;

public interface CategoryQueryPort {
    /**
     * Returns all the user's categories, active and inactive, ordered by name and then by id.
     * Returns an empty list when the user has no categories.
     */
    List<CategoryDetails> findByUserId(UUID userId);
}
