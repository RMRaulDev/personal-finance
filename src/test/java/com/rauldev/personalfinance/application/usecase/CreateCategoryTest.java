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

import com.rauldev.personalfinance.application.port.out.CategoryRepository;
import com.rauldev.personalfinance.application.port.out.TransactionManager;
import com.rauldev.personalfinance.domain.Category;
import com.rauldev.personalfinance.domain.CategoryStatus;
import com.rauldev.personalfinance.domain.CategoryType;

class CreateCategoryTest {
    @Test
    void execute_shouldCreateCategoryAndPersistIt() {
        UUID userId = UUID.randomUUID();
        String categoryName = "Salary";

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);
        UUID result = createCategory.execute(new CreateCategoryCommand(userId, categoryName, CategoryType.INCOME));

        assertNotNull(result);
        assertTrue(transactionManager.executed);
        assertEquals(1, categoryRepository.existsCalls);
        assertEquals(1, categoryRepository.createCalls);
        assertNotNull(categoryRepository.createdCategory);
        assertEquals(userId, categoryRepository.createdCategory.userId());
        assertEquals(categoryName, categoryRepository.createdCategory.name());
        assertEquals(CategoryType.INCOME, categoryRepository.createdCategory.type());
        assertEquals(CategoryStatus.ACTIVE, categoryRepository.createdCategory.status());
        assertEquals(result, categoryRepository.createdCategory.id());
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenCategoryNameAlreadyExistsForUser() {
        UUID userId = UUID.randomUUID();
        String categoryName = "Salary";

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(true);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> createCategory.execute(new CreateCategoryCommand(userId, categoryName, CategoryType.INCOME)));

        assertEquals(1, categoryRepository.existsCalls);
        assertEquals(0, categoryRepository.createCalls);
        assertTrue(categoryRepository.createdCategory == null);
        assertTrue(transactionManager.executed);
    }

    @Test
    void execute_shouldAllowSameNameForAnotherUser() {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);
        UUID result = createCategory.execute(new CreateCategoryCommand(firstUserId, "Salary", CategoryType.INCOME));
        UUID secondResult = createCategory.execute(new CreateCategoryCommand(secondUserId, "Salary", CategoryType.INCOME));

        assertNotNull(result);
        assertNotNull(secondResult);
        assertEquals(2, categoryRepository.existsCalls);
        assertEquals(2, categoryRepository.createCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenNameIsEmpty() {
        UUID userId = UUID.randomUUID();

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> createCategory.execute(new CreateCategoryCommand(userId, "", CategoryType.INCOME)));

        assertEquals(1, categoryRepository.existsCalls);
        assertEquals(0, categoryRepository.createCalls);
    }

    @Test
    void execute_shouldPropagateDomainExceptionWhenNameIsBlank() {
        UUID userId = UUID.randomUUID();

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> createCategory.execute(new CreateCategoryCommand(userId, "   ", CategoryType.INCOME)));

        assertEquals(1, categoryRepository.existsCalls);
        assertEquals(0, categoryRepository.createCalls);
    }

    @Test
    void execute_shouldRejectNullCommand() {
        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);

        assertThrows(NullPointerException.class, () -> createCategory.execute(null));
    }

    @Test
    void command_shouldRejectNullReferences() {
        assertThrows(NullPointerException.class,
            () -> new CreateCategoryCommand(null, "Salary", CategoryType.INCOME));
        assertThrows(NullPointerException.class,
            () -> new CreateCategoryCommand(UUID.randomUUID(), null, CategoryType.INCOME));
        assertThrows(NullPointerException.class,
            () -> new CreateCategoryCommand(UUID.randomUUID(), "Salary", null));
    }

    @Test
    void constructor_shouldRejectNullDependencies() {
        assertThrows(NullPointerException.class,
            () -> new CreateCategory(null, null));
        assertThrows(NullPointerException.class,
            () -> new CreateCategory(null, new RecordingTransactionManager()));
        assertThrows(NullPointerException.class,
            () -> new CreateCategory(new RecordingCategoryRepository(false), null));
    }

    @Test
    void execute_shouldUseTransactionManager() {
        UUID userId = UUID.randomUUID();

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(false);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);
        createCategory.execute(new CreateCategoryCommand(userId, "Salary", CategoryType.INCOME));

        assertTrue(transactionManager.executed);
    }

    @Test
    void execute_shouldNotPersistWhenBusinessValidationFails() {
        UUID userId = UUID.randomUUID();

        RecordingCategoryRepository categoryRepository = new RecordingCategoryRepository(true);
        RecordingTransactionManager transactionManager = new RecordingTransactionManager();

        CreateCategory createCategory = new CreateCategory(categoryRepository, transactionManager);

        assertThrows(IllegalArgumentException.class,
            () -> createCategory.execute(new CreateCategoryCommand(userId, "Salary", CategoryType.INCOME)));

        assertEquals(1, categoryRepository.existsCalls);
        assertEquals(0, categoryRepository.createCalls);
    }

    private static final class RecordingTransactionManager implements TransactionManager {
        private boolean executed;

        @Override
        public <T> T execute(Supplier<T> transactionalWork) {
            executed = true;
            return transactionalWork.get();
        }
    }

    private static final class RecordingCategoryRepository implements CategoryRepository {
        private final boolean duplicate;
        private int existsCalls;
        private int createCalls;
        private Category createdCategory;

        private RecordingCategoryRepository(boolean duplicate) {
            this.duplicate = duplicate;
        }

        @Override
        public Category create(Category category) {
            createCalls++;
            createdCategory = category;
            return category;
        }

        @Override
        public Optional<Category> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
            return Optional.empty();
        }

        @Override
        public List<Category> findByUserId(UUID userId) {
            return List.of();
        }

        @Override
        public boolean existsByUserIdAndName(UUID userId, String name) {
            existsCalls++;
            return duplicate;
        }

        @Override
        public Category update(Category category) {
            return category;
        }
    }
}
