package com.rauldev.personalfinance.entry.web.category;

import java.util.UUID;

import com.rauldev.personalfinance.application.usecase.CreateCategoryCommand;
import com.rauldev.personalfinance.domain.CategoryType;
import com.rauldev.personalfinance.entry.web.common.RequiredFields;

/**
 * Body of {@code POST /api/v1/categories}. {@code type} is {@code INCOME} or {@code EXPENSE}; an
 * unknown value is rejected with 400 while the JSON body is read.
 */
public record CreateCategoryRequest(String name, CategoryType type) {

    /**
     * @throws IllegalArgumentException if {@code name} or {@code type} is missing
     */
    public CreateCategoryCommand toCommand(UUID userId) {
        return new CreateCategoryCommand(
            userId, RequiredFields.require(name, "name"), RequiredFields.require(type, "type"));
    }
}
