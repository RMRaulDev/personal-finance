package com.rauldev.personalfinance.application.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

class CreateAccountTest {
    @Test
    void execute_shouldCreateAccountAndPersistIt() {
        UUID userId = UUID.randomUUID();
        String accountName = "Checking";

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateAccount createAccount = new CreateAccount(
            accountRepository,
            transactionManager
        );

        UUID result = createAccount.execute(new CreateAccountCommand(userId, accountName));

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, accountRepository.existsCalls);
        assertEquals(1, accountRepository.createCalls);
        assertEquals(List.of("exists", "create"), accountRepository.callOrder);
        assertNotNull(accountRepository.createdAccount);
        assertEquals(userId, accountRepository.createdAccount.userId());
        assertEquals(accountName, accountRepository.createdAccount.name());
        assertEquals(Money.ofCents(0), accountRepository.createdAccount.balance());
        assertEquals(AccountStatus.ACTIVE, accountRepository.createdAccount.status());
        assertEquals(result, accountRepository.createdAccount.id());
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenAccountNameAlreadyExists() {
        UUID userId = UUID.randomUUID();
        String accountName = "Checking";

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(true);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateAccount createAccount = new CreateAccount(
            accountRepository,
            transactionManager
        );

        assertThrows(IllegalArgumentException.class,
            () -> createAccount.execute(new CreateAccountCommand(userId, accountName)));

        assertEquals(1, accountRepository.existsCalls);
        assertEquals(0, accountRepository.createCalls);
        assertEquals(List.of("exists"), accountRepository.callOrder);
        assertTrue(accountRepository.createdAccount == null);
        assertTrue(transactionManager.executed);
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new CreateAccountCommand(null, "Checking"));
        assertThrows(NullPointerException.class,
            () -> new CreateAccountCommand(UUID.randomUUID(), null));
    }

    @Test
    void execute_shouldRejectNullCommand() {
        RecordingAccountRepository accountRepository = new RecordingAccountRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateAccount createAccount = new CreateAccount(
            accountRepository,
            transactionManager
        );

        assertThrows(NullPointerException.class,
            () -> createAccount.execute(null));
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new CreateAccount(null, null));
        assertThrows(NullPointerException.class,
            () -> new CreateAccount(null, new RecordingTransactionManager()));
        assertThrows(NullPointerException.class,
            () -> new CreateAccount(new RecordingAccountRepository(false), null));
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateAccount createAccount = new CreateAccount(
            accountRepository,
            transactionManager
        );

        createAccount.execute(new CreateAccountCommand(userId, "Checking"));

        assertTrue(transactionManager.executed);
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            return transactionalWork.get();
        }
    }

    private static final class RecordingAccountRepository implements AccountRepository {
        private final boolean duplicate;
        private final List<String> callOrder = new ArrayList<>();
        private int existsCalls;
        private int createCalls;
        private Account createdAccount;

        private RecordingAccountRepository(boolean duplicate) {
            this.duplicate = duplicate;
        }

        @Override
        public Account create(Account account) {
            createCalls++;
            callOrder.add("create");
            createdAccount = account;
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<Account> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            existsCalls++;
            callOrder.add("exists");
            return duplicate;
        }

        @Override
        public Account update(Account account) {
            return account;
        }
    }
}
