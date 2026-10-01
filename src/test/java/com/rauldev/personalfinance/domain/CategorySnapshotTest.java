package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CategorySnapshotTest {

    @Test
    void rejectsNullFieldsWithTheirMessages() {
        assertEquals("Category id cannot be null", assertThrows(NullPointerException.class,
            () -> new CategorySnapshot(null, CategoryStatus.ACTIVE)).getMessage());
        assertEquals("Category status cannot be null", assertThrows(NullPointerException.class,
            () -> new CategorySnapshot(UUID.randomUUID(), null)).getMessage());
    }
}
