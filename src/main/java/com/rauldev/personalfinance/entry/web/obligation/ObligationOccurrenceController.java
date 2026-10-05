package com.rauldev.personalfinance.entry.web.obligation;

import java.net.URI;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rauldev.personalfinance.application.usecase.PayOccurrence;
import com.rauldev.personalfinance.application.usecase.ReopenOccurrence;
import com.rauldev.personalfinance.application.usecase.ReopenOccurrenceCommand;
import com.rauldev.personalfinance.application.usecase.SkipOccurrence;
import com.rauldev.personalfinance.application.usecase.SkipOccurrenceCommand;
import com.rauldev.personalfinance.application.usecase.SkipOverdueOccurrences;
import com.rauldev.personalfinance.application.usecase.SkipOverdueOccurrencesCommand;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;

/**
 * Occurrences of one of the current user's obligations: pay, skip, reopen, and skip every overdue
 * occurrence.
 *
 * <p>An occurrence is addressed by its due date in the path ({@code .../occurrences/2026-10-15/pay}).
 * {@link PathVariable} converts the segment to a {@link LocalDate} using ISO-8601
 * ({@code spring.mvc.format.date=iso}); an invalid date answers 400 before the method runs. For pay
 * and skip, a date that is valid but not on the obligation's calendar answers 409
 * {@code OCCURRENCE_NOT_SCHEDULED}. Reopen does not check the calendar: a date without a resolution
 * answers 404.
 *
 * <p>Paying is <strong>not idempotent</strong> in general (it registers an expense), but paying
 * or skipping the same occurrence twice is rejected with 409 {@code OCCURRENCE_ALREADY_RESOLVED},
 * so a retried payment of the same occurrence cannot register a second expense. To undo a payment,
 * cancel its expense with {@code POST /api/v1/operations/{operationId}/cancel}; the occurrence
 * becomes pending (or overdue) again. Reopening a paid occurrence answers 409
 * {@code OCCURRENCE_PAID_NOT_REOPENABLE}.
 */
@RestController
@RequestMapping("/api/v1/obligations/{obligationId}")
public class ObligationOccurrenceController {
    private static final PayOccurrenceRequest NO_OVERRIDES = new PayOccurrenceRequest(null, null, null, null);

    private final PayOccurrence payOccurrence;
    private final SkipOccurrence skipOccurrence;
    private final SkipOverdueOccurrences skipOverdueOccurrences;
    private final ReopenOccurrence reopenOccurrence;
    private final CurrentUserProvider currentUserProvider;

    public ObligationOccurrenceController(
        PayOccurrence payOccurrence,
        SkipOccurrence skipOccurrence,
        SkipOverdueOccurrences skipOverdueOccurrences,
        ReopenOccurrence reopenOccurrence,
        CurrentUserProvider currentUserProvider
    ) {
        this.payOccurrence = Objects.requireNonNull(payOccurrence, "Pay occurrence cannot be null");
        this.skipOccurrence = Objects.requireNonNull(skipOccurrence, "Skip occurrence cannot be null");
        this.skipOverdueOccurrences = Objects.requireNonNull(
            skipOverdueOccurrences, "Skip overdue occurrences cannot be null");
        this.reopenOccurrence = Objects.requireNonNull(reopenOccurrence, "Reopen occurrence cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    /**
     * Pays one occurrence by registering an expense and answers {@code 201 Created} with the
     * expense's {@code Location} ({@code /api/v1/operations/{operationId}}) and its id.
     *
     * <p>The body is optional: {@code @RequestBody(required = false)} makes Spring pass
     * {@code null} when the request has no body, instead of answering 400. Without a body the
     * payment uses every default (see {@link PayOccurrenceRequest}). A body that is present must
     * still be valid JSON ({@code {}} is valid and also means every default).
     */
    @PostMapping("/occurrences/{dueDate}/pay")
    public ResponseEntity<OccurrencePaymentResponse> pay(
        @PathVariable UUID obligationId,
        @PathVariable LocalDate dueDate,
        @RequestBody(required = false) PayOccurrenceRequest request
    ) {
        PayOccurrenceRequest payment = request == null ? NO_OVERRIDES : request;
        UUID operationId = payOccurrence.execute(
            payment.toCommand(currentUserProvider.currentUserId(), obligationId, dueDate));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/v1/operations/{operationId}")
            .buildAndExpand(operationId)
            .toUri();
        return ResponseEntity.created(location).body(new OccurrencePaymentResponse(operationId));
    }

    /**
     * Skips one occurrence (future dates of the calendar included) and answers
     * {@code 204 No Content}.
     */
    @PostMapping("/occurrences/{dueDate}/skip")
    public ResponseEntity<Void> skip(@PathVariable UUID obligationId, @PathVariable LocalDate dueDate) {
        skipOccurrence.execute(new SkipOccurrenceCommand(currentUserProvider.currentUserId(), obligationId, dueDate));
        return ResponseEntity.noContent().build();
    }

    /**
     * Reopens a skipped occurrence and answers {@code 204 No Content}. A paid occurrence is
     * reopened by cancelling its expense instead (see the class documentation).
     */
    @PostMapping("/occurrences/{dueDate}/reopen")
    public ResponseEntity<Void> reopen(@PathVariable UUID obligationId, @PathVariable LocalDate dueDate) {
        reopenOccurrence.execute(
            new ReopenOccurrenceCommand(currentUserProvider.currentUserId(), obligationId, dueDate));
        return ResponseEntity.noContent().build();
    }

    /**
     * Skips every overdue occurrence in one transaction and answers {@code 200 OK} with the
     * skipped due dates. With nothing overdue it changes nothing and answers an empty list.
     */
    @PostMapping("/overdue-occurrences/skip")
    public SkippedOccurrencesResponse skipOverdue(@PathVariable UUID obligationId) {
        return new SkippedOccurrencesResponse(skipOverdueOccurrences.execute(
            new SkipOverdueOccurrencesCommand(currentUserProvider.currentUserId(), obligationId)));
    }
}
