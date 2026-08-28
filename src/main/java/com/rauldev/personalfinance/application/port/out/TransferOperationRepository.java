package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.rauldev.personalfinance.domain.Transfer;

public interface TransferOperationRepository {
    Transfer create(Transfer transfer);

    Optional<Transfer> findById(UUID id);

    Optional<Transfer> findByIdAndUserId(UUID id, UUID userId);

    List<Transfer> findByUserId(UUID userId);

    void deleteById(UUID id);
}
