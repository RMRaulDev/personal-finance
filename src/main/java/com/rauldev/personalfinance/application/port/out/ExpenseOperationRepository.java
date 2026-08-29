package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Expense;

public interface ExpenseOperationRepository {
    Expense create(Expense expense);

    Optional<Expense> findById(UUID id);

    Optional<Expense> findByIdAndUserId(UUID id, UUID userId);

    List<Expense> findByUserId(UUID userId);

    Expense update(Expense expense);

    void deleteById(UUID id);
}
