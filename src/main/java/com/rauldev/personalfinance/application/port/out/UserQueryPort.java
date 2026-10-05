package com.rauldev.personalfinance.application.port.out;

import java.util.UUID;

public interface UserQueryPort {
    boolean existsById(UUID userId);
}
