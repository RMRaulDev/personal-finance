package com.rauldev.personalfinance.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.readmodel.AccountSummary;
import com.rauldev.personalfinance.application.readmodel.CategorySummary;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationDetails;
import com.rauldev.personalfinance.application.readmodel.FinancialOperationHistoryItem;
import com.rauldev.personalfinance.application.readmodel.TransferDetails;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.OperationStatus;

class ReadModelValidationTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 24);

    @Test
    void accountSummaryRequiresIdAndName() {
        assertDoesNotThrow(() -> new AccountSummary(UUID.randomUUID(), "Checking"));

        assertThrows(NullPointerException.class, () -> new AccountSummary(null, "Checking"));
        assertThrows(NullPointerException.class, () -> new AccountSummary(UUID.randomUUID(), null));
    }

    @Test
    void categorySummaryRequiresIdAndName() {
        assertDoesNotThrow(() -> new CategorySummary(UUID.randomUUID(), "Food"));

        assertThrows(NullPointerException.class, () -> new CategorySummary(null, "Food"));
        assertThrows(NullPointerException.class, () -> new CategorySummary(UUID.randomUUID(), null));
    }

    @Test
    void transferDetailsRequiresBothAccounts() {
        AccountSummary source = new AccountSummary(UUID.randomUUID(), "Source");
        AccountSummary target = new AccountSummary(UUID.randomUUID(), "Target");

        assertDoesNotThrow(() -> new TransferDetails(source, target));
        assertThrows(NullPointerException.class, () -> new TransferDetails(null, target));
        assertThrows(NullPointerException.class, () -> new TransferDetails(source, null));
    }

    @Test
    void financialOperationHistoryItemAcceptsValidIncomeExpenseAndTransfer() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.INCOME,
            Money.of(BigDecimal.valueOf(150.00)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.EXPENSE,
            Money.of(BigDecimal.valueOf(75.50)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(),
            OperationType.TRANSFER,
            Money.of(BigDecimal.valueOf(200.00)),
            OPERATION_DATE,
            null,
            null,
            null,
            null,
            transfer));
    }

    @Test
    void financialOperationHistoryItemRejectsInvalidRequiredFieldsAndTypeRules() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            null, OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), null, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, null, OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), null, OperationStatus.ACTIVE, null, account, category, null));

        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, null, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.EXPENSE, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.EXPENSE, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, account, null, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, null, null));
        assertThrows(NullPointerException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, null, null, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, null, account, category, null));
        assertDoesNotThrow(() -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, Instant.now(), account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationHistoryItem(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, Instant.now(), account, category, null));
    }

    @Test
    void financialOperationDetailsHasSameInvariantsAsHistoryItem() {
        AccountSummary account = new AccountSummary(UUID.randomUUID(), "Checking");
        CategorySummary category = new CategorySummary(UUID.randomUUID(), "Food");
        TransferDetails transfer = new TransferDetails(
            new AccountSummary(UUID.randomUUID(), "Source"),
            new AccountSummary(UUID.randomUUID(), "Target"));

        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(),
            OperationType.INCOME,
            Money.of(BigDecimal.valueOf(100.00)),
            OPERATION_DATE,
            OperationStatus.ACTIVE,
            null,
            account,
            category,
            null));

        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(),
            OperationType.TRANSFER,
            Money.of(BigDecimal.valueOf(250.00)),
            OPERATION_DATE,
            null,
            null,
            null,
            null,
            transfer));

        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, null, account, category, transfer));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.TRANSFER, Money.of(BigDecimal.TEN), OPERATION_DATE, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.ACTIVE, Instant.now(), account, category, null));
        assertThrows(IllegalArgumentException.class, () -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, null, account, category, null));
        assertDoesNotThrow(() -> new FinancialOperationDetails(
            UUID.randomUUID(), OperationType.INCOME, Money.of(BigDecimal.TEN), OPERATION_DATE, OperationStatus.CANCELLED, Instant.now(), account, category, null));
    }
}
