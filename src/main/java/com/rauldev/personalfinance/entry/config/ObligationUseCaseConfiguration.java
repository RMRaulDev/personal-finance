package com.rauldev.personalfinance.entry.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.ExpenseOperationRepository;
import com.rauldev.personalfinance.application.port.out.ObligationRepository;
import com.rauldev.personalfinance.application.port.out.OccurrenceResolutionRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.usecase.ArchiveObligation;
import com.rauldev.personalfinance.application.usecase.CreateObligation;
import com.rauldev.personalfinance.application.usecase.ExpenseRegistration;
import com.rauldev.personalfinance.application.usecase.ModifyObligation;
import com.rauldev.personalfinance.application.usecase.PayOccurrence;
import com.rauldev.personalfinance.application.usecase.ReopenOccurrence;
import com.rauldev.personalfinance.application.usecase.SkipOccurrence;
import com.rauldev.personalfinance.application.usecase.SkipOverdueOccurrences;

/**
 * Wires the obligation use cases: lifecycle (create, modify, archive) and occurrences (pay, skip,
 * skip overdue, reopen).
 *
 * <p>Ports come from {@link PersistenceConfiguration}; the {@link Clock} that decides "today"
 * comes from {@link ClockConfiguration} (tests replace it with a {@code @Primary} fixed clock).
 *
 * <p>The {@link ExpenseRegistration} bean exists only for {@link PayOccurrence}, which receives it
 * by constructor and runs it inside its own transaction. {@code RegisterExpense} does not use this
 * bean: it builds its own {@code ExpenseRegistration} internally.
 */
@Configuration(proxyBeanMethods = false)
public class ObligationUseCaseConfiguration {

    @Bean
    public ExpenseRegistration expenseRegistration(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ExpenseOperationRepository expenseOperationRepository
    ) {
        return new ExpenseRegistration(accountRepository, categoryRepository, expenseOperationRepository);
    }

    @Bean
    public CreateObligation createObligation(
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        ObligationRepository obligationRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new CreateObligation(
            accountRepository, categoryRepository, obligationRepository, transactionManager, clock);
    }

    @Bean
    public ModifyObligation modifyObligation(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        AccountRepository accountRepository,
        CategoryRepository categoryRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new ModifyObligation(
            obligationRepository,
            occurrenceResolutionRepository,
            accountRepository,
            categoryRepository,
            transactionManager,
            clock);
    }

    @Bean
    public ArchiveObligation archiveObligation(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new ArchiveObligation(obligationRepository, occurrenceResolutionRepository, transactionManager, clock);
    }

    @Bean
    public SkipOccurrence skipOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new SkipOccurrence(obligationRepository, occurrenceResolutionRepository, transactionManager, clock);
    }

    @Bean
    public SkipOverdueOccurrences skipOverdueOccurrences(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new SkipOverdueOccurrences(
            obligationRepository, occurrenceResolutionRepository, transactionManager, clock);
    }

    @Bean
    public ReopenOccurrence reopenOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        TransactionManager transactionManager
    ) {
        return new ReopenOccurrence(obligationRepository, occurrenceResolutionRepository, transactionManager);
    }

    @Bean
    public PayOccurrence payOccurrence(
        ObligationRepository obligationRepository,
        OccurrenceResolutionRepository occurrenceResolutionRepository,
        ExpenseRegistration expenseRegistration,
        TransactionManager transactionManager,
        Clock clock
    ) {
        return new PayOccurrence(
            obligationRepository, occurrenceResolutionRepository, expenseRegistration, transactionManager, clock);
    }
}
