package com.rauldev.personalfinance.entry.web.category;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.usecase.CreateCategoryCommand;
import com.rauldev.personalfinance.domain.CategoryType;

class CreateCategoryRequestTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Test
    void passesNameTypeAndUserToCommand() {
        CreateCategoryCommand command = new CreateCategoryRequest("Food", CategoryType.EXPENSE).toCommand(USER_ID);

        assertEquals(USER_ID, command.userId());
        assertEquals("Food", command.name());
        assertEquals(CategoryType.EXPENSE, command.type());
    }

    @Test
    void rejectsMissingName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new CreateCategoryRequest(null, CategoryType.EXPENSE).toCommand(USER_ID));

        assertEquals("Field 'name' is required", e.getMessage());
    }

    @Test
    void rejectsMissingType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new CreateCategoryRequest("Food", null).toCommand(USER_ID));

        assertEquals("Field 'type' is required", e.getMessage());
    }
}
