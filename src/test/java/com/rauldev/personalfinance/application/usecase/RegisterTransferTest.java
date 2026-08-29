package com.rauldev.personalfinance.application.usecase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.rauldev.personalfinance.application.port.out.TransferOperationRepository;
import com.rauldev.personalfinance.domain.Account;
import com.rauldev.personalfinance.domain.Money;
import com.rauldev.personalfinance.domain.Transfer;

class RegisterTransferTest {
    private static final LocalDate OPERATION_DATE = LocalDate.of(2026, 8, 20);

    @Test
    void execute_shouldRegisterTransferAndUpdateAccounts() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Money amount = Money.ofCents(3000);

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(10000));
        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            amount,
            OPERATION_DATE
        );

        UUID result = registerTransfer.execute(command);

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(2, accountRepository.findCalls.size());
        assertEquals(sourceAccountId, accountRepository.findCalls.get(0));
        assertEquals(targetAccountId, accountRepository.findCalls.get(1));
        assertEquals(1, transferRepository.createCalls);
        assertEquals(2, accountRepository.updatedAccounts.size());
        assertNotNull(transferRepository.createdTransfer);
        assertEquals(sourceAccountId, transferRepository.createdTransfer.sourceAccountId());
        assertEquals(targetAccountId, transferRepository.createdTransfer.targetAccountId());
        assertEquals(amount, transferRepository.createdTransfer.amount());
        assertEquals(result, transferRepository.createdTransfer.id());
        assertEquals(Money.ofCents(7000), sourceAccount.balance());
        assertEquals(Money.ofCents(5000), targetAccount.balance());
        assertEquals(sourceAccountId, accountRepository.updatedAccounts.get(0).id());
        assertEquals(targetAccountId, accountRepository.updatedAccounts.get(1).id());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenSourceAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();

        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            Money.ofCents(1000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerTransfer.execute(command));
        assertEquals(1, accountRepository.findCalls.size());
        assertEquals(sourceAccountId, accountRepository.findCalls.get(0));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(2000), targetAccount.balance());
    }

    @Test
    void execute_shouldThrowResourceNotFoundExceptionWhenTargetAccountDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(5000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            Money.ofCents(1000),
            OPERATION_DATE
        );

        assertThrows(ResourceNotFoundException.class, () -> registerTransfer.execute(command));
        assertEquals(2, accountRepository.findCalls.size());
        assertEquals(sourceAccountId, accountRepository.findCalls.get(0));
        assertEquals(targetAccountId, accountRepository.findCalls.get(1));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(5000), sourceAccount.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenSourceAndTargetAccountsAreTheSame() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Money amount = Money.ofCents(1000);

        Account account = new Account(accountId, userId, "Checking");
        account.credit(Money.ofCents(5000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(account);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            accountId,
            accountId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerTransfer.execute(command));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(5000), account.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenSourceAccountHasInsufficientBalance() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Money amount = Money.ofCents(5000);

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(2000));
        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(1000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalStateException.class, () -> registerTransfer.execute(command));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(2000), sourceAccount.balance());
        assertEquals(Money.ofCents(1000), targetAccount.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenSourceAccountIsInactive() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Money amount = Money.ofCents(1000);

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(5000));
        sourceAccount.deactivate();

        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalStateException.class, () -> registerTransfer.execute(command));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(5000), sourceAccount.balance());
        assertEquals(Money.ofCents(2000), targetAccount.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenTargetAccountIsInactive() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();
        Money amount = Money.ofCents(1000);

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(5000));

        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));
        targetAccount.deactivate();

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            amount,
            OPERATION_DATE
        );

        assertThrows(IllegalStateException.class, () -> registerTransfer.execute(command));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(5000), sourceAccount.balance());
        assertEquals(Money.ofCents(2000), targetAccount.balance());
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenAmountIsInvalid() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(5000));
        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        RegisterTransferCommand command = new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            Money.ofCents(0),
            OPERATION_DATE
        );

        assertThrows(IllegalArgumentException.class, () -> registerTransfer.execute(command));
        assertEquals(0, transferRepository.createCalls);
        assertTrue(accountRepository.updatedAccounts.isEmpty());
        assertEquals(Money.ofCents(5000), sourceAccount.balance());
        assertEquals(Money.ofCents(2000), targetAccount.balance());
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();
        UUID sourceAccountId = UUID.randomUUID();
        UUID targetAccountId = UUID.randomUUID();

        Account sourceAccount = new Account(sourceAccountId, userId, "Checking");
        sourceAccount.credit(Money.ofCents(5000));
        Account targetAccount = new Account(targetAccountId, userId, "Savings");
        targetAccount.credit(Money.ofCents(2000));

        RecordingAccountRepository accountRepository = new RecordingAccountRepository(sourceAccount, targetAccount);
        RecordingTransferRepository transferRepository = new RecordingTransferRepository();
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        RegisterTransfer registerTransfer = new RegisterTransfer(
            accountRepository,
            transferRepository,
            transactionManager
        );

        registerTransfer.execute(new RegisterTransferCommand(
            userId,
            sourceAccountId,
            targetAccountId,
            Money.ofCents(1000),
            OPERATION_DATE
        ));

        assertTrue(transactionManager.executed);
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new RegisterTransfer(null, null, null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new RegisterTransferCommand(null, UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterTransferCommand(UUID.randomUUID(), null, UUID.randomUUID(), Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterTransferCommand(UUID.randomUUID(), UUID.randomUUID(), null, Money.ofCents(100), OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterTransferCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, OPERATION_DATE));
        assertThrows(NullPointerException.class,
            () -> new RegisterTransferCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Money.ofCents(100), null));
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
        private final Map<UUID, Account> accounts = new HashMap<>();
        private final List<UUID> findCalls = new ArrayList<>();
        private final List<Account> updatedAccounts = new ArrayList<>();

        private RecordingAccountRepository(Account... initialAccounts) {
            for (Account account : initialAccounts) {
                if (account != null) {
                    accounts.put(account.id(), account);
                }
            }
        }

        @Override
        public Account create(Account account) {
            return account;
        }

        @Override
        public Optional<Account> findById(UUID id) {
            return Optional.ofNullable(accounts.get(id));
        }

        @Override
        public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
            findCalls.add(id);
            Account account = accounts.get(id);
            if (account != null && account.userId().equals(userId)) {
                return Optional.of(account);
            }
            return Optional.empty();
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
        public Account update(Account account) {
            updatedAccounts.add(account);
            return account;
        }
    }

    private static final class RecordingTransferRepository implements TransferOperationRepository {
        private int createCalls;
        private Transfer createdTransfer;

        @Override
        public Transfer create(Transfer transfer) {
            createCalls++;
            createdTransfer = transfer;
            return transfer;
        }

        @Override
        public Optional<Transfer> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Transfer> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<Transfer> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
        }
    }
}
