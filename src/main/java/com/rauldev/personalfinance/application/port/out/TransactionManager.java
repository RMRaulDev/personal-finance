package com.rauldev.personalfinance.application.port.out;

import java.util.Objects;
import java.util.function.Supplier;

public interface TransactionManager {
    <T> T execute(Supplier<T> transactionalWork);

    default void execute(Runnable transactionalWork) {
        Objects.requireNonNull(transactionalWork, "Transactional work cannot be null");
        execute(() -> {
            transactionalWork.run();
            return null;
        });
    }
}
