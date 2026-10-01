package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OperationReferencesTest {
    private static final String INVALID_TYPE_MESSAGE = "Invalid type for test";

    private final UUID userId = UUID.randomUUID();
    private final Account account = new Account(userId, "Checking");
    private final Category expenseCategory = new Category(userId, "Food", CategoryType.EXPENSE);

    private static void assertRule(BusinessRuleCode code, String message, Runnable action) {
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, action::run);
        assertEquals(code, exception.code());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void acceptsActiveReferencesOfTheSameUserAndExpectedType() {
        assertDoesNotThrow(() -> OperationReferences.validate(account, expenseCategory, CategoryType.EXPENSE,
            INVALID_TYPE_MESSAGE));
    }

    @Test
    void acceptsIncomeCategoryWhenIncomeIsExpected() {
        Category income = new Category(userId, "Salary", CategoryType.INCOME);

        assertDoesNotThrow(() -> OperationReferences.validate(account, income, CategoryType.INCOME,
            INVALID_TYPE_MESSAGE));
    }

    @Test
    void rejectsNullAccount() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> OperationReferences.validate(null, expenseCategory, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Account cannot be null", exception.getMessage());
    }

    @Test
    void rejectsNullCategory() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> OperationReferences.validate(account, null, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Category cannot be null", exception.getMessage());
    }

    @Test
    void checksNullAccountBeforeNullCategory() {
        NullPointerException exception = assertThrows(NullPointerException.class,
            () -> OperationReferences.validate(null, null, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Account cannot be null", exception.getMessage());
    }

    @Test
    void rejectsAccountAndCategoryOfDifferentUsers() {
        Category foreign = new Category(UUID.randomUUID(), "Food", CategoryType.EXPENSE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, foreign, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void rejectsCategoryOfUnexpectedTypeWithTheGivenMessage() {
        Category income = new Category(userId, "Salary", CategoryType.INCOME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, income, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals(INVALID_TYPE_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsExpenseCategoryWhenIncomeIsExpected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, expenseCategory, CategoryType.INCOME, INVALID_TYPE_MESSAGE));

        assertEquals(INVALID_TYPE_MESSAGE, exception.getMessage());
    }

    @Test
    void rejectsInactiveAccount() {
        account.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active",
            () -> OperationReferences.validate(account, expenseCategory, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));
    }

    @Test
    void rejectsInactiveCategory() {
        expenseCategory.deactivate();

        assertRule(BusinessRuleCode.CATEGORY_INACTIVE, "Category must be active",
            () -> OperationReferences.validate(account, expenseCategory, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));
    }

    @Test
    void checksNullsBeforeSameUser() {
        Category foreign = new Category(UUID.randomUUID(), "Food", CategoryType.EXPENSE);

        assertThrows(NullPointerException.class,
            () -> OperationReferences.validate(null, foreign, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));
        assertThrows(NullPointerException.class,
            () -> OperationReferences.validate(account, null, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));
    }

    @Test
    void checksSameUserBeforeCategoryType() {
        Category foreignIncome = new Category(UUID.randomUUID(), "Salary", CategoryType.INCOME);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, foreignIncome, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void checksCategoryTypeBeforeInactiveAccount() {
        Category income = new Category(userId, "Salary", CategoryType.INCOME);
        account.deactivate();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, income, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals(INVALID_TYPE_MESSAGE, exception.getMessage());
    }

    @Test
    void checksSameUserBeforeInactiveReferences() {
        Category foreign = new Category(UUID.randomUUID(), "Food", CategoryType.EXPENSE);
        account.deactivate();
        foreign.deactivate();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> OperationReferences.validate(account, foreign, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));

        assertEquals("Account and category must belong to the same user", exception.getMessage());
    }

    @Test
    void checksInactiveAccountBeforeInactiveCategory() {
        account.deactivate();
        expenseCategory.deactivate();

        assertRule(BusinessRuleCode.ACCOUNT_INACTIVE, "Account must be active",
            () -> OperationReferences.validate(account, expenseCategory, CategoryType.EXPENSE, INVALID_TYPE_MESSAGE));
    }
}
