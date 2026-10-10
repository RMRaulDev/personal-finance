package com.rauldev.personalfinance.application.readmodel;

/**
 * State of one occurrence of an obligation on a given day: {@code OVERDUE} and {@code PENDING} are unresolved
 * (before today, and today or later), {@code PAID} and {@code SKIPPED} have a resolution.
 */
public enum OccurrenceStatus {
    OVERDUE,
    PENDING,
    PAID,
    SKIPPED
}
