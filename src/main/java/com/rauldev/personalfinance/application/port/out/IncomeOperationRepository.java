package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Income;

public interface IncomeOperationRepository {
    Income create(Income income);

    Optional<Income> findById(UUID id);

    Optional<Income> findByIdAndUserId(UUID id, UUID userId);

    List<Income> findByUserId(UUID userId);

    Income update(Income income);

    void deleteById(UUID id);
}
