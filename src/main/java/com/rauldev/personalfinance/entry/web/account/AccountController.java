package com.rauldev.personalfinance.entry.web.account;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rauldev.personalfinance.application.usecase.CreateAccount;
import com.rauldev.personalfinance.application.usecase.GetAccount;
import com.rauldev.personalfinance.application.usecase.GetAccountQuery;
import com.rauldev.personalfinance.application.usecase.ModifyAccount;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.entry.web.common.IdResponse;

/**
 * Accounts of the current user.
 *
 * <p>{@link RestController} combines {@code @Controller} and {@code @ResponseBody}: return values
 * are written as JSON instead of being resolved as view names. Spring creates this controller
 * through component scanning (it lives in the entry layer) and injects the use case beans through
 * its single constructor, so no {@code @Autowired} is needed.
 *
 * <p>{@link ResponseEntity} is returned where the status or headers differ from the default
 * {@code 200 OK} (201 with {@code Location}, 204 without body).
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    private final CreateAccount createAccount;
    private final ModifyAccount modifyAccount;
    private final GetAccount getAccount;
    private final CurrentUserProvider currentUserProvider;

    public AccountController(
        CreateAccount createAccount,
        ModifyAccount modifyAccount,
        GetAccount getAccount,
        CurrentUserProvider currentUserProvider
    ) {
        this.createAccount = Objects.requireNonNull(createAccount, "Create account cannot be null");
        this.modifyAccount = Objects.requireNonNull(modifyAccount, "Modify account cannot be null");
        this.getAccount = Objects.requireNonNull(getAccount, "Get account cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Creates an account and answers {@code 201 Created}. The {@code Location} URI is built from
     * the current request's scheme, host, port, and context path
     * ({@link ServletUriComponentsBuilder#fromCurrentContextPath()}), so it stays correct wherever
     * the application is deployed.
     */
    @PostMapping
    public ResponseEntity<IdResponse> create(@RequestBody CreateAccountRequest request) {
        UUID accountId = createAccount.execute(request.toCommand(currentUserProvider.currentUserId()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/accounts/{accountId}")
            .buildAndExpand(accountId)
            .toUri();
        return ResponseEntity.created(location).body(new IdResponse(accountId));
    }

    /**
     * Renames an account and answers {@code 204 No Content}. {@link PathVariable} converts the
     * path segment to a {@link UUID}; an invalid UUID answers 400 before this method runs.
     */
    @PatchMapping("/{accountId}")
    public ResponseEntity<Void> modify(
        @PathVariable UUID accountId,
        @RequestBody ModifyAccountRequest request
    ) {
        modifyAccount.execute(request.toCommand(currentUserProvider.currentUserId(), accountId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable UUID accountId) {
        return AccountResponse.from(
            getAccount.execute(new GetAccountQuery(currentUserProvider.currentUserId(), accountId)));
    }
}
