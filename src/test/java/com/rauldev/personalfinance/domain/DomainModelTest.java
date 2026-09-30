package com.rauldev.personalfinance.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DomainModelTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);
    private static final Instant CANCELLED_AT = Instant.parse("2026-08-25T12:00:00Z");

    @Test
    void userHasUuidIdentity() {
        User user = new User();

        assertNotNull(user.id());
        assertEquals(user.id(), new User(user.id()).id());
        assertEquals(user, new User(user.id()));
    }

    @Test
    void accountStartsActiveWithZeroBalance() {
        Account account = new Account(UUID.randomUUID(), "Checking");

        assertEquals(AccountStatus.ACTIVE, account.status());
        assertTrue(account.balance().isZero());
    }

    @Test
    void accountMaintainsNonNegativeBalance() {
        Account account = new Account(UUID.randomUUID(), "Checking");
        account.credit(Money.ofCents(1000));
        account.debit(Money.ofCents(400));

        assertEquals(Money.ofCents(600), account.balance());

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> account.debit(Money.ofCents(601)));
        assertEquals(BusinessRuleCode.INSUFFICIENT_BALANCE, exception.code());
        assertEquals("Account balance is insufficient", exception.getMessage());
        assertEquals(Money.ofCents(600), account.balance());
    }

    @Test
    void accountAllowsDebitEqualToBalance() {
        Account account = new Account(UUID.randomUUID(), "Checking");
        account.credit(Money.ofCents(1000));

        account.debit(Money.ofCents(1000));

        assertTrue(account.balance().isZero());
    }

    @Test
    void accountRejectsInvalidNamesAndSupportsStatusChanges() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(userId, "Checking");

        assertThrows(IllegalArgumentException.class, () -> new Account(userId, " "));
        account.rename("Savings");
        account.deactivate();
        assertEquals(AccountStatus.INACTIVE, account.status());
        account.activate();
        assertEquals(AccountStatus.ACTIVE, account.status());
    }

    @Test
    void categoryTypeIsImmutableAndStatusCanChange() {
        Category category = new Category(UUID.randomUUID(), "Salary", CategoryType.INCOME);

        assertEquals(CategoryType.INCOME, category.type());
        category.deactivate();
        assertEquals(CategoryStatus.INACTIVE, category.status());
        assertFalse(category.status() == CategoryStatus.ACTIVE);
        assertThrows(IllegalArgumentException.class,
            () -> new Category(category.userId(), "", CategoryType.EXPENSE));
    }

    @Test
    void operationsRequirePositiveAmountAndDate() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
            () -> new Income(userId, Money.ofCents(0), OPERATION_DATE, accountId, categoryId));
        assertThrows(IllegalArgumentException.class,
            () -> new Expense(userId, Money.ofCents(0), OPERATION_DATE, accountId, categoryId));
        assertThrows(IllegalArgumentException.class,
            () -> new Transfer(userId, Money.ofCents(0), OPERATION_DATE, accountId, UUID.randomUUID()));
        assertThrows(NullPointerException.class,
            () -> new Income(userId, Money.ofCents(1), null, accountId, categoryId));
    }

    @Test
    void incomeAndExpenseValidateCategoryAndUser() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(userId, "Checking");
        Category incomeCategory = new Category(userId, "Salary", CategoryType.INCOME);
        Category expenseCategory = new Category(userId, "Food", CategoryType.EXPENSE);

        Income income = Income.register(account, incomeCategory, Money.ofCents(1), OPERATION_DATE);
        assertEquals(OperationStatus.ACTIVE, income.status());
        income.cancel();
        assertEquals(OperationStatus.CANCELLED, income.status());
        assertThrows(IllegalArgumentException.class,
            () -> Expense.register(account, incomeCategory, Money.ofCents(1), OPERATION_DATE));
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> Expense.register(account, expenseCategory, Money.ofCents(1), OPERATION_DATE));
        assertEquals(BusinessRuleCode.INSUFFICIENT_BALANCE, exception.code());
        assertEquals("Account balance is insufficient", exception.getMessage());
    }

    @Test
    void expenseAllowsAmountEqualToAccountBalance() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(userId, "Checking");
        Category category = new Category(userId, "Food", CategoryType.EXPENSE);
        account.credit(Money.ofCents(1000));

        Expense expense = Expense.register(account, category, Money.ofCents(1000), OPERATION_DATE);

        assertEquals(Money.ofCents(1000), expense.amount());
    }

    @Test
    void transferRequiresActiveSameUserAccountsAndSufficientBalance() {
        UUID userId = UUID.randomUUID();
        Account source = new Account(userId, "Checking");
        Account target = new Account(userId, "Savings");
        source.credit(Money.ofCents(1000));

        Transfer transfer = Transfer.register(source, target, Money.ofCents(500), OPERATION_DATE);
        assertEquals(source.id(), transfer.sourceAccountId());
        assertEquals(target.id(), transfer.targetAccountId());
        assertThrows(IllegalArgumentException.class,
            () -> Transfer.register(source, source, Money.ofCents(1), OPERATION_DATE));
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> Transfer.register(source, target, Money.ofCents(1001), OPERATION_DATE));
        assertEquals(BusinessRuleCode.INSUFFICIENT_BALANCE, exception.code());
        assertEquals("Source account balance is insufficient", exception.getMessage());
    }

    @Test
    void transferAllowsAmountEqualToSourceBalance() {
        UUID userId = UUID.randomUUID();
        Account source = new Account(userId, "Checking");
        Account target = new Account(userId, "Savings");
        source.credit(Money.ofCents(1000));

        Transfer transfer = Transfer.register(source, target, Money.ofCents(1000), OPERATION_DATE);

        assertEquals(Money.ofCents(1000), transfer.amount());
    }

    @Test
    void transferRejectsInactiveSourceAccount() {
        UUID userId = UUID.randomUUID();
        Account source = new Account(userId, "Checking");
        Account target = new Account(userId, "Savings");
        source.credit(Money.ofCents(1000));
        source.deactivate();

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> Transfer.register(source, target, Money.ofCents(1), OPERATION_DATE));

        assertEquals(BusinessRuleCode.ACCOUNT_INACTIVE, exception.code());
        assertEquals("Both accounts must be active", exception.getMessage());
    }

    @Test
    void transferRejectsInactiveTargetAccount() {
        UUID userId = UUID.randomUUID();
        Account source = new Account(userId, "Checking");
        Account target = new Account(userId, "Savings");
        source.credit(Money.ofCents(1000));
        target.deactivate();

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class,
            () -> Transfer.register(source, target, Money.ofCents(1), OPERATION_DATE));

        assertEquals(BusinessRuleCode.ACCOUNT_INACTIVE, exception.code());
        assertEquals("Both accounts must be active", exception.getMessage());
    }

    @Test
    void transferHasNoStatusOrCategory() {
        for (var method : Transfer.class.getDeclaredMethods()) {
            assertFalse(method.getName().equals("status"));
            assertFalse(method.getName().equals("categoryId"));
        }
    }

    @Test
    void reversalCopiesOnlyIncomeOrExpenseIdentityAndAmount() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(userId, "Checking");
        Category category = new Category(userId, "Salary", CategoryType.INCOME);
        Income income = Income.register(account, category, Money.ofCents(1000), OPERATION_DATE);
        Reversal reversal = new Reversal(income, CANCELLED_AT);

        assertEquals(userId, reversal.userId());
        assertEquals(income.id(), reversal.originalOperationId());
        assertEquals(income.amount(), reversal.amount());
        assertEquals(CANCELLED_AT, reversal.cancelledAt());
        assertThrows(IllegalArgumentException.class,
            () -> new Reversal(new Transfer(userId, Money.ofCents(1), OPERATION_DATE,
                account.id(), UUID.randomUUID()), CANCELLED_AT));
    }

    @Test
    void reversalHasNoOperationDateStatusOrCategory() {
        for (var method : Reversal.class.getDeclaredMethods()) {
            assertFalse(method.getName().equals("operationDate"));
            assertFalse(method.getName().equals("status"));
            assertFalse(method.getName().equals("categoryId"));
        }
    }
}
