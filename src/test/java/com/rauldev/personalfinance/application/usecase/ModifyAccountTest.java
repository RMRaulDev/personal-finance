package com.rauldev.personalfinance.application.usecase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.application.port.out.AccountRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.AccountStatus;
import com.rauldev.personalfinance.domain.Money;

class ModifyAccountTest {
    @Test
    void execute_shouldModifyAccountNameAndPersistIt() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);
        UUID result = modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Savings"));

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, accountRepository.duplicateCheckCalls);
        assertEquals(1, accountRepository.updateCalls);
        assertEquals("Savings", account.name());
        assertEquals(accountId, result);
        assertEquals(accountRepository.updatedAccount, account);
    }

    @Test
    void execute_shouldPreserveBalanceAndStatusWhenRenamingAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(2500));
        account.deactivate();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);
        modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Savings"));

        assertEquals(Money.ofCents(2500), account.balance());
        assertEquals(AccountStatus.INACTIVE, account.status());
        assertEquals("Savings", account.name());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.empty(), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);

        assertThrows(ResourceNotFoundException.class,
            () -> modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Savings")));

        assertEquals(1, accountRepository.findCalls);
        assertEquals(0, accountRepository.duplicateCheckCalls);
        assertEquals(0, accountRepository.updateCalls);
        assertTrue(transactionManager.executed);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenAnotherAccountUsesSameName() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), true);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Savings")));

        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, accountRepository.duplicateCheckCalls);
        assertEquals(0, accountRepository.updateCalls);
        assertEquals("Checking", account.name());
    }

    @Test
    void execute_shouldAllowSameAccountToKeepItsOwnName() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);
        UUID result = modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Checking"));

        assertEquals(accountId, result);
        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, accountRepository.duplicateCheckCalls);
        assertEquals(1, accountRepository.updateCalls);
        assertEquals("Checking", account.name());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenNewNameIsBlank() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "")));

        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, accountRepository.duplicateCheckCalls);
        assertEquals(0, accountRepository.updateCalls);
        assertEquals("Checking", account.name());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenNewNameIsWhitespace() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "   ")));

        assertEquals(1, accountRepository.findCalls);
        assertEquals(1, accountRepository.duplicateCheckCalls);
        assertEquals(0, accountRepository.updateCalls);
        assertEquals("Checking", account.name());
    }

    @Test
    void execute_shouldRejectNullCommand() {
        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(
            new Account(UUID.randomUUID(), UUID.randomUUID(), "Checking")
        ), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);

        assertThrows(NullPointerException.class, () -> modifyAccount.execute(null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new ModifyAccountCommand(null, UUID.randomUUID(), "Checking"));
        assertThrows(NullPointerException.class,
            () -> new ModifyAccountCommand(UUID.randomUUID(), null, "Checking"));
        assertThrows(NullPointerException.class,
            () -> new ModifyAccountCommand(UUID.randomUUID(), UUID.randomUUID(), null));
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new ModifyAccount(null, null));
        assertThrows(NullPointerException.class,
            () -> new ModifyAccount(null, new RecordingTransactionManager()));
        assertThrows(NullPointerException.class,
            () -> new ModifyAccount(new RecordingAccountRepository(Optional.of(
                new Account(UUID.randomUUID(), UUID.randomUUID(), "Checking")
            ), false), null));
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Account account = new Account(accountId, userId, "Checking");

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(Optional.of(account), false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        ModifyAccount modifyAccount = new ModifyAccount(accountRepository, transactionManager);
        modifyAccount.execute(new ModifyAccountCommand(userId, accountId, "Savings"));

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
        private final Optional<Account> account;
        private final boolean duplicateOtherName;
        private int findCalls;
        private int duplicateCheckCalls;
        private int updateCalls;
        private Account updatedAccount;

        private RecordingAccountRepository(Optional<Account> account, boolean duplicateOtherName) {
            this.account = account;
            this.duplicateOtherName = duplicateOtherName;
        }

        @Override
        public Account create(Account account) {
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return account.filter(found -> found.id().equals(id));
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            findCalls++;
            return account.filter(found -> found.id().equals(id) && found.userId().equals(userId));
        }

        @Override
        public List<Account> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            return false;
        }

        @Override
        public boolean existsByUserIdAndNameAndIdNot(UUID userId, String name, UUID accountId) {
            duplicateCheckCalls++;
            if (account.isEmpty()) {
                return false;
            }
            if (account.get().id().equals(accountId) && account.get().name().equals(name)) {
                return false;
            }
            return duplicateOtherName || account.get().name().equals(name) && !account.get().id().equals(accountId);
        }

        @Override
        public Account update(Account account) {
            updateCalls++;
            updatedAccount = account;
            return account;
        }
    }
}
