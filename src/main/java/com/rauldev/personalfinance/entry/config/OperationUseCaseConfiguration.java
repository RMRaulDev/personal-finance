package com.rauldev.personalfinance.entry.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.FinancialOperationQueryPort;
import com.rauldev.personalfinance.application.port.out.IncomeOperationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.ReversalRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.port.out.TransferOperationRepository;
import com.rauldev.personalfinance.application.usecase.CancelOperation;
import com.rauldev.personalfinance.application.usecase.GetOperationDetails;
import com.rauldev.personalfinance.application.usecase.GetOperationHistory;
import com.rauldev.personalfinance.application.usecase.RegisterExpense;
import com.rauldev.personalfinance.application.usecase.RegisterIncome;
import com.rauldev.personalfinance.application.usecase.RegisterTransfer;

/**
 * Wires the financial operation use cases (incomes, expenses, transfers, cancellation, history,
 * and details).
 *
 * <p>Ports come from {@link PersistenceConfiguration}; the {@link Clock} that
 * {@link CancelOperation} uses for the reversal timestamp comes from {@link ClockConfiguration}.
 * Declaring the {@code Clock} as a parameter (instead of calling {@code Clock.systemUTC()} here)
 * lets tests replace it with a fixed clock through a {@code @Primary} bean.
 */
@Configuration(proxyBeanMethods = false)
public class OperationUseCaseConfiguration {

    @Bean
    public RegisterIncome registerIncome(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        IncomeOperationRepository incomeOperationRepository,
        TransactionManager transactionManager
    ) {
        return new RegisterIncome(accountRepository, categoryRepository, incomeOperationRepository, transactionManager);
    }

    @Bean
    public RegisterExpense registerExpense(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ExpenseOperationRepository expenseOperationRepository,
        TransactionManager transactionManager
    ) {
        return new RegisterExpense(accountRepository, categoryRepository, expenseOperationRepository, transactionManager);
    }

    @Bean
    public RegisterTransfer registerTransfer(
        AccountRepository accountRepository,
        TransferOperationRepository transferOperationRepository,
        TransactionManager transactionManager
    ) {
        return new RegisterTransfer(accountRepository, transferOperationRepository, transactionManager);
    }

    @Bean
    public CancelOperation cancelOperation(
        AccountRepository accountRepository,
        IncomeOperationRepository incomeOperationRepository,
        ExpenseOperationRepository expenseOperationRepository,
        ReversalRepository reversalRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new CancelOperation(
            accountRepository,
            incomeOperationRepository,
            expenseOperationRepository,
            reversalRepository,
            occurrenceResolutionRepository,
            transactionManager,
            clock);
    }

    @Bean
    public GetOperationHistory getOperationHistory(FinancialOperationQueryPort financialOperationQueryPort) {
        return new GetOperationHistory(financialOperationQueryPort);
    }

    @Bean
    public GetOperationDetails getOperationDetails(FinancialOperationQueryPort financialOperationQueryPort) {
        return new GetOperationDetails(financialOperationQueryPort);
    }
}
