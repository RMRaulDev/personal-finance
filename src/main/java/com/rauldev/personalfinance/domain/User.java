package com.rauldev.personalfinance.domain;

import java.util.Objects;
import java.util.UUID;

public final class User {
    private final UUID id;

    public User() {
        this(UUID.randomUUID());
    }

    public User(UUID id) {
        this.id = Objects.requireNonNull(id, "User id cannot be null");
    }

    public UUID id() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
