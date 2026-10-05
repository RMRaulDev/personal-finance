package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Objects;

import com.rauldev.personalfinance.application.port.out.CategoryQueryPort;
import com.rauldev.personalfinance.application.readmodel.CategoryDetails;

public final class ListCategories {
    private final CategoryQueryPort categoryQueryPort;

    public ListCategories(CategoryQueryPort categoryQueryPort) {
        this.categoryQueryPort = Objects.requireNonNull(categoryQueryPort, "Category query port cannot be null");
    }

    /**
     * Returns all the user's categories, active and inactive, ordered by name and then by id.
     */
    public List<CategoryDetails> execute(ListCategoriesQuery query) {
        Objects.requireNonNull(query, "Query cannot be null");
        return categoryQueryPort.findByUserId(query.userId());
    }
}
