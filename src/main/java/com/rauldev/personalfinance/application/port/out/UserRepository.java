package com.rauldev.personalfinance.application.port.out;

import java.util.UUID;

import com.rauldev.personalfinance.domain.User;

public interface UserRepository {
    User create(User user);

    void deleteById(UUID userId);
}
