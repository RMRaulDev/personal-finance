package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

public final class Category {
    private final UUID id;
    private final UUID userId;
    private String name;
    private final CategoryType type;
    private CategoryStatus status;

    public Category(UUID userId, String name, CategoryType type) {
        this(UUID.randomUUID(), userId, name, type);
    }

    public Category(UUID id, UUID userId, String name, CategoryType type) {
        this.id = Objects.requireNonNull(id, "Category id cannot be null");
        this.userId = Objects.requireNonNull(userId, "User id cannot be null");
        this.name = validateName(name);
        this.type = Objects.requireNonNull(type, "Category type cannot be null");
        this.status = CategoryStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String name() {
        return name;
    }

    public CategoryType type() {
        return type;
    }

    public CategoryStatus status() {
        return status;
    }

    public void activate() {
        status = CategoryStatus.ACTIVE;
    }

    public void deactivate() {
        status = CategoryStatus.INACTIVE;
    }

    public void rename(String name) {
        this.name = validateName(name);
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "Category name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        return name;
    }
}
