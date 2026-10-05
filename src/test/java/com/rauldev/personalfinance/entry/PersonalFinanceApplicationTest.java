package com.rauldev.personalfinance.entry;

import java.nio.file.Path;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.application.usecase.CancelOperation;
import com.rauldev.personalfinance.application.usecase.CreateAccount;
import com.rauldev.personalfinance.application.usecase.CreateCategory;
import com.rauldev.personalfinance.application.usecase.GetAccount;
import com.rauldev.personalfinance.application.usecase.GetOperationDetails;
import com.rauldev.personalfinance.application.usecase.GetOperationHistory;
import com.rauldev.personalfinance.application.usecase.ListAccounts;
import com.rauldev.personalfinance.application.usecase.ListCategories;
import com.rauldev.personalfinance.application.usecase.ModifyAccount;
import com.rauldev.personalfinance.application.usecase.RegisterExpense;
import com.rauldev.personalfinance.application.usecase.RegisterIncome;
import com.rauldev.personalfinance.application.usecase.RegisterTransfer;
import com.rauldev.personalfinance.entry.security.ConfiguredSingleUserProvider;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcAccountQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcAccountRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcCategoryQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcCategoryRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcExpenseOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcFinancialOperationQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcIncomeOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcOccurrenceResolutionRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcReversalRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcTransferOperationRepository;
import com.rauldev.personalfinance.infrastructure.persistence.JdbcUserQueryAdapter;
import com.rauldev.personalfinance.infrastructure.persistence.SQLiteConnectionProvider;
import com.rauldev.personalfinance.infrastructure.transaction.JdbcTransactionManager;
import com.rauldev.personalfinance.infrastructure.transaction.TransactionConnectionHolder;

@SpringBootTest
class PersonalFinanceApplicationTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteUrl(DynamicPropertyRegistry registry) {
        registry.add("personal-finance.sqlite.url",
            () -> "jdbc:sqlite:" + tempDir.resolve("startup.db").toAbsolutePath());
        registry.add("personal-finance.single-user-id", () -> USER_ID.toString());
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void contextContainsPersistenceBeans() {
        assertEquals(1, context.getBeansOfType(SQLiteConnectionProvider.class).size());
        assertEquals(1, context.getBeansOfType(TransactionConnectionHolder.class).size());
        assertInstanceOf(JdbcTransactionManager.class, context.getBean(TransactionManager.class));
    }

    @Test
    void contextExposesConfiguredSingleUserProvider() {
        assertEquals(1, context.getBeansOfType(CurrentUserProvider.class).size());
        assertInstanceOf(ConfiguredSingleUserProvider.class, context.getBean(CurrentUserProvider.class));
    }

    @Test
    void contextHasNoDataSourceBean() {
        assertEquals(0, context.getBeanNamesForType(DataSource.class).length);
    }

    @Test
    void startupDoesNotCreateDatabaseFile() {
        assertFalse(tempDir.resolve("startup.db").toFile().exists());
    }

    @Test
    void onlyExplicitlyWiredCoreClassesAreBeans() {
        Set<Class<?>> allowed = Set.of(
            SQLiteConnectionProvider.class, TransactionConnectionHolder.class, JdbcTransactionManager.class,
            JdbcUserQueryAdapter.class, JdbcAccountRepository.class, JdbcCategoryRepository.class,
            JdbcAccountQueryAdapter.class, CreateAccount.class, ModifyAccount.class, GetAccount.class,
            CreateCategory.class, JdbcCategoryQueryAdapter.class, ListAccounts.class, ListCategories.class,
            JdbcIncomeOperationRepository.class, JdbcExpenseOperationRepository.class,
            JdbcTransferOperationRepository.class, JdbcReversalRepository.class,
            JdbcOccurrenceResolutionRepository.class, JdbcFinancialOperationQueryAdapter.class,
            RegisterIncome.class, RegisterExpense.class, RegisterTransfer.class, CancelOperation.class,
            GetOperationHistory.class, GetOperationDetails.class);

        for (String name : context.getBeanDefinitionNames()) {
            Class<?> type = context.getType(name);
            if (type == null || allowed.contains(type)) {
                continue;
            }
            String pkg = type.getPackageName();
            for (String layer : List.of("domain", "application", "infrastructure")) {
                assertFalse(pkg.startsWith("com.rauldev.personalfinance." + layer),
                    "Unexpected core bean '" + name + "' of type " + type.getName());
            }
        }
    }
}
