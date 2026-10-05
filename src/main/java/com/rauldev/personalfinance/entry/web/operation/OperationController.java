package com.rauldev.personalfinance.entry.web.operation;

import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rauldev.personalfinance.application.query.OperationSearchCriteria;
import com.rauldev.personalfinance.application.query.OperationType;
import com.rauldev.personalfinance.application.usecase.CancelOperation;
import com.rauldev.personalfinance.application.usecase.CancelOperationCommand;
import com.rauldev.personalfinance.application.usecase.GetOperationDetails;
import com.rauldev.personalfinance.application.usecase.GetOperationDetailsQuery;
import com.rauldev.personalfinance.application.usecase.GetOperationHistory;
import com.rauldev.personalfinance.application.usecase.RegisterExpense;
import com.rauldev.personalfinance.application.usecase.RegisterIncome;
import com.rauldev.personalfinance.application.usecase.RegisterTransfer;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.entry.web.common.IdResponse;

/**
 * Financial operations (incomes, expenses, transfers) of the current user.
 *
 * <p>Registrations are <strong>not idempotent</strong>: each {@code POST} creates a new operation
 * and moves the balance, so a client that retries after a timeout may register the operation
 * twice. There is no idempotency key yet; a client that is unsure should check the history before
 * retrying.
 */
@RestController
@RequestMapping("/api/v1/operations")
public class OperationController {
    private final RegisterIncome registerIncome;
    private final RegisterExpense registerExpense;
    private final RegisterTransfer registerTransfer;
    private final CancelOperation cancelOperation;
    private final GetOperationHistory getOperationHistory;
    private final GetOperationDetails getOperationDetails;
    private final CurrentUserProvider currentUserProvider;

    public OperationController(
        RegisterIncome registerIncome,
        RegisterExpense registerExpense,
        RegisterTransfer registerTransfer,
        CancelOperation cancelOperation,
        GetOperationHistory getOperationHistory,
        GetOperationDetails getOperationDetails,
        CurrentUserProvider currentUserProvider
    ) {
        this.registerIncome = Objects.requireNonNull(registerIncome, "Register income cannot be null");
        this.registerExpense = Objects.requireNonNull(registerExpense, "Register expense cannot be null");
        this.registerTransfer = Objects.requireNonNull(registerTransfer, "Register transfer cannot be null");
        this.cancelOperation = Objects.requireNonNull(cancelOperation, "Cancel operation cannot be null");
        this.getOperationHistory = Objects.requireNonNull(getOperationHistory, "Get operation history cannot be null");
        this.getOperationDetails = Objects.requireNonNull(getOperationDetails, "Get operation details cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Registers an income, credits the account, and answers {@code 201 Created} with the
     * operation's {@code Location}. Not idempotent (see the class documentation).
     */
    @PostMapping("/incomes")
    public ResponseEntity<IdResponse> registerIncome(@RequestBody RegisterIncomeRequest request) {
        return created(registerIncome.execute(request.toCommand(currentUserProvider.currentUserId())));
    }

    /**
     * Registers an expense, debits the account, and answers {@code 201 Created} with the
     * operation's {@code Location}. Not idempotent (see the class documentation).
     */
    @PostMapping("/expenses")
    public ResponseEntity<IdResponse> registerExpense(@RequestBody RegisterExpenseRequest request) {
        return created(registerExpense.execute(request.toCommand(currentUserProvider.currentUserId())));
    }

    /**
     * Registers a transfer between two accounts of the current user and answers
     * {@code 201 Created} with the operation's {@code Location}. Not idempotent (see the class
     * documentation).
     */
    @PostMapping("/transfers")
    public ResponseEntity<IdResponse> registerTransfer(@RequestBody RegisterTransferRequest request) {
        return created(registerTransfer.execute(request.toCommand(currentUserProvider.currentUserId())));
    }

    /**
     * Cancels an income or expense and answers {@code 204 No Content}. The request has no body,
     * and the id of the recorded reversal is not returned (there is no reversal resource); the
     * cancellation shows up as {@code status = CANCELLED} and {@code cancelledAt} on the operation.
     *
     * <p>Transfers cannot be cancelled: cancelling one answers {@code 404} ("Operation not found
     * or not cancelable"), the same as an unknown operation, because the Core only looks the id up
     * among incomes and expenses.
     */
    @PostMapping("/{operationId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID operationId) {
        cancelOperation.execute(new CancelOperationCommand(currentUserProvider.currentUserId(), operationId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Answers one page of the current user's operations, ordered by operation date then id, both
     * descending (newest first).
     *
     * <p>Every query parameter is optional. {@link RequestParam} binds a parameter by its name;
     * with {@code required = false} a missing parameter arrives as {@code null} instead of
     * answering 400. Spring converts the text to the parameter type: {@link UUID}s, ISO-8601 dates
     * ({@code spring.mvc.format.date=iso}), and the {@link OperationType} constant name
     * ({@code INCOME}, {@code EXPENSE}, {@code TRANSFER}; case-sensitive). A value that cannot be
     * converted answers 400 before this method runs. {@code page} and {@code pageSize} are
     * {@link Integer} so that their absence can be detected; the defaults come from
     * {@link OperationSearchCriteria#forUser(UUID)} (page 1, 20 items), and the
     * {@link OperationSearchCriteria} constructor validates the values (page &ge; 1, page size
     * 1..100, {@code from} not after {@code to}; otherwise 400).
     */
    @GetMapping
    public OperationPageResponse history(
        @RequestParam(required = false) UUID accountId,
        @RequestParam(required = false) UUID categoryId,
        @RequestParam(required = false) OperationType type,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer pageSize
    ) {
        OperationSearchCriteria defaults = OperationSearchCriteria.forUser(currentUserProvider.currentUserId());
        OperationSearchCriteria criteria = new OperationSearchCriteria(
            defaults.userId(),
            accountId,
            categoryId,
            type,
            from,
            to,
            page == null ? defaults.page() : page,
            pageSize == null ? defaults.pageSize() : pageSize);

        return new OperationPageResponse(
            getOperationHistory.execute(criteria).stream().map(OperationResponse::from).toList(),
            criteria.page(),
            criteria.pageSize());
    }

    @GetMapping("/{operationId}")
    public OperationResponse get(@PathVariable UUID operationId) {
        return OperationResponse.from(getOperationDetails.execute(
            new GetOperationDetailsQuery(currentUserProvider.currentUserId(), operationId)));
    }

    private static ResponseEntity<IdResponse> created(UUID operationId) {
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/operations/{operationId}")
            .buildAndExpand(operationId)
            .toUri();
        return ResponseEntity.created(location).body(new IdResponse(operationId));
    }
}
