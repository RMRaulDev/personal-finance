package com.rauldev.personalfinance.entry.web.obligation;

import java.net.URI;
import java.util.List;
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

import com.rauldev.personalfinance.application.usecase.ArchiveObligation;
import com.rauldev.personalfinance.application.usecase.ArchiveObligationCommand;
import com.rauldev.personalfinance.application.usecase.CreateObligation;
import com.rauldev.personalfinance.application.usecase.GetObligation;
import com.rauldev.personalfinance.application.usecase.GetObligationQuery;
import com.rauldev.personalfinance.application.usecase.ListObligations;
import com.rauldev.personalfinance.application.usecase.ListObligationsQuery;
import com.rauldev.personalfinance.application.usecase.ModifyObligation;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;
import com.rauldev.personalfinance.entry.web.common.IdResponse;

/**
 * Lifecycle of the current user's obligations (payment commitments): create, modify, archive, list,
 * and get. The occurrences of an obligation (pay, skip, reopen) are handled by
 * {@link ObligationOccurrenceController}.
 *
 * <p>Creating is not idempotent: a client that retries after a timeout may get a 409
 * {@code OBLIGATION_NAME_ALREADY_EXISTS} if the first request did succeed.
 */
@RestController
@RequestMapping("/api/v1/obligations")
public class ObligationController {
    private final CreateObligation createObligation;
    private final ModifyObligation modifyObligation;
    private final ArchiveObligation archiveObligation;
    private final ListObligations listObligations;
    private final GetObligation getObligation;
    private final CurrentUserProvider currentUserProvider;

    public ObligationController(
        CreateObligation createObligation,
        ModifyObligation modifyObligation,
        ArchiveObligation archiveObligation,
        ListObligations listObligations,
        GetObligation getObligation,
        CurrentUserProvider currentUserProvider
    ) {
        this.createObligation = Objects.requireNonNull(createObligation, "Create obligation cannot be null");
        this.modifyObligation = Objects.requireNonNull(modifyObligation, "Modify obligation cannot be null");
        this.archiveObligation = Objects.requireNonNull(archiveObligation, "Archive obligation cannot be null");
        this.listObligations = Objects.requireNonNull(listObligations, "List obligations cannot be null");
        this.getObligation = Objects.requireNonNull(getObligation, "Get obligation cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Creates an active obligation and answers {@code 201 Created} with a {@code Location} header
     * pointing to {@code GET /api/v1/obligations/{obligationId}} (built like in
     * {@code AccountController}).
     */
    @PostMapping
    public ResponseEntity<IdResponse> create(@RequestBody CreateObligationRequest request) {
        UUID obligationId = createObligation.execute(request.toCommand(currentUserProvider.currentUserId()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/obligations/{obligationId}")
            .buildAndExpand(obligationId)
            .toUri();
        return ResponseEntity.created(location).body(new IdResponse(obligationId));
    }

    /**
     * Partially updates an obligation and answers {@code 204 No Content}: absent fields keep their
     * value, and a supplied {@code recurrence} replaces the whole calendar (see
     * {@link ModifyObligationRequest}).
     */
    @PatchMapping("/{obligationId}")
    public ResponseEntity<Void> modify(
        @PathVariable UUID obligationId,
        @RequestBody ModifyObligationRequest request
    ) {
        modifyObligation.execute(request.toCommand(currentUserProvider.currentUserId(), obligationId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Archives an obligation and answers {@code 204 No Content}. It fails with 409 while the
     * obligation has overdue occurrences; pay or skip them first.
     */
    @PostMapping("/{obligationId}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID obligationId) {
        archiveObligation.execute(new ArchiveObligationCommand(currentUserProvider.currentUserId(), obligationId));
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists all the current user's obligations, active and archived, ordered by name then id
     * (case-sensitive, see application.md → Query Rules). The body is a bare JSON array (empty when
     * the user has no obligations), like {@code GET /api/v1/accounts}.
     */
    @GetMapping
    public List<ObligationResponse> list() {
        return listObligations.execute(new ListObligationsQuery(currentUserProvider.currentUserId())).stream()
            .map(ObligationResponse::from)
            .toList();
    }

    /**
     * Returns one obligation of the current user; a missing obligation or another user's answers 404.
     */
    @GetMapping("/{obligationId}")
    public ObligationResponse get(@PathVariable UUID obligationId) {
        return ObligationResponse.from(
            getObligation.execute(new GetObligationQuery(currentUserProvider.currentUserId(), obligationId)));
    }
}
