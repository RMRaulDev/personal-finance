package com.rauldev.personalfinance.entry.web.dashboard;

import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rauldev.personalfinance.application.usecase.GetDashboard;
import com.rauldev.personalfinance.application.usecase.GetDashboardQuery;
import com.rauldev.personalfinance.entry.security.CurrentUserProvider;

/**
 * Dashboard of the current user: available to spend, items that need attention, upcoming
 * commitments, and recent activity. "Today" comes from the configured time zone
 * ({@code personal-finance.time-zone}).
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final GetDashboard getDashboard;
    private final CurrentUserProvider currentUserProvider;

    public DashboardController(GetDashboard getDashboard, CurrentUserProvider currentUserProvider) {
        this.getDashboard = Objects.requireNonNull(getDashboard, "Get dashboard cannot be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "Current user provider cannot be null");
    }

    @GetMapping
    public DashboardResponse get() {
        return DashboardResponse.from(getDashboard.execute(new GetDashboardQuery(currentUserProvider.currentUserId())));
    }
}
