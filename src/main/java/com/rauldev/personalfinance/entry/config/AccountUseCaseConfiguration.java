package com.rauldev.personalfinance.entry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rauldev.personalfinance.application.port.out.AccountQueryPort;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.CategoryQueryPort;
import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.usecase.CreateAccount;
import com.rauldev.personalfinance.application.usecase.CreateCategory;
import com.rauldev.personalfinance.application.usecase.GetAccount;
import com.rauldev.personalfinance.application.usecase.ListAccounts;
import com.rauldev.personalfinance.application.usecase.ListCategories;
import com.rauldev.personalfinance.application.usecase.ModifyAccount;

/**
 * Wires the account and category use cases.
 *
 * <p>Use cases carry no Spring annotations, so they are created here with explicit
 * {@code @Bean} methods. Their ports are received as method parameters; Spring resolves them from
 * the beans declared in {@link PersistenceConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
public class AccountUseCaseConfiguration {

    @Bean
    public CreateAccount createAccount(
        AccountRepository accountRepository,
        TransactionManager transactionManager
    ) {
        return new CreateAccount(accountRepository, transactionManager);
    }

    @Bean
    public ModifyAccount modifyAccount(
        AccountRepository accountRepository,
        TransactionManager transactionManager
    ) {
        return new ModifyAccount(accountRepository, transactionManager);
    }

    @Bean
    public GetAccount getAccount(AccountQueryPort accountQueryPort) {
        return new GetAccount(accountQueryPort);
    }

    @Bean
    public ListAccounts listAccounts(AccountQueryPort accountQueryPort) {
        return new ListAccounts(accountQueryPort);
    }

    @Bean
    public CreateCategory createCategory(
        CategoryRepository categoryRepository,
        TransactionManager transactionManager
    ) {
        return new CreateCategory(categoryRepository, transactionManager);
    }

    @Bean
    public ListCategories listCategories(CategoryQueryPort categoryQueryPort) {
        return new ListCategories(categoryQueryPort);
    }
}
