package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ResolutionSnapshotTest {

    @Test
    void rejectsNullFieldsWithTheirMessages() {
        assertEquals("Obligation id cannot be null", assertThrows(NullPointerException.class,
            () -> new ResolutionSnapshot(null, LocalDate.of(2026, 8, 20))).getMessage());
        assertEquals("Due date cannot be null", assertThrows(NullPointerException.class,
            () -> new ResolutionSnapshot(UUID.randomUUID(), null)).getMessage());
    }
}
