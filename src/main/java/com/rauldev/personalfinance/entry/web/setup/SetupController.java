package com.rauldev.personalfinance.entry.web.setup;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rauldev.personalfinance.application.usecase.CompleteInitialSetup;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.entry.web.common.IdResponse;

/**
 * Initial setup of the current user: the first account, with its opening balance, and the default
 * categories.
 */
@RestController
@RequestMapping("/api/v1/setup")
public class SetupController {
    private final CompleteInitialSetup completeInitialSetup;
    private final CurrentUserProvider currentUserProvider;

    public SetupController(
        CompleteInitialSetup completeInitialSetup,
        CurrentUserProvider currentUserProvider
    ) {
        this.completeInitialSetup = Objects.requireNonNull(completeInitialSetup,
            "Complete initial setup cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Completes the setup and answers {@code 201 Created} with the new account's id and its
     * {@code Location}. A user that already has an account or a category answers 409
     * {@code INITIAL_SETUP_ALREADY_COMPLETED}, so a retry creates nothing.
     */
    @PostMapping
    public ResponseEntity<IdResponse> complete(@RequestBody CompleteInitialSetupRequest request) {
        UUID accountId = completeInitialSetup.execute(request.toCommand(currentUserProvider.currentUserId()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/accounts/{accountId}")
            .buildAndExpand(accountId)
            .toUri();
        return ResponseEntity.created(location).body(new IdResponse(accountId));
    }
}
