package com.rauldev.personalfinance.application.port.out;

import java.util.List;
import java.util.UUID;

import com.rauldev.personalfinance.application.readmodel.RecentActivityItem;
import com.rauldev.personalfinance.domain.AccountSnapshot;
import com.rauldev.personalfinance.domain.CategorySnapshot;
import com.rauldev.personalfinance.domain.ObligationSnapshot;
import com.rauldev.personalfinance.domain.ResolutionSnapshot;

/**
 * Reads the data of a user's dashboard. Every method is scoped by user.
 */
public interface DashboardQueryPort {
    List<ObligationSnapshot> findActiveObligations(UUID userId);

    /**
     * @return the PAID and SKIPPED resolutions of the user's ACTIVE obligations
     */
    List<ResolutionSnapshot> findResolutionsOfActiveObligations(UUID userId);

    /**
     * @return the user's accounts, ACTIVE and INACTIVE
     */
    List<AccountSnapshot> findAccounts(UUID userId);

    /**
     * @return the user's categories, ACTIVE and INACTIVE
     */
    List<CategorySnapshot> findCategories(UUID userId);

    /**
     * @param limit maximum number of items, at least 1
     * @return the user's most recent ACTIVE operations, newest first
     */
    List<RecentActivityItem> findRecentActivity(UUID userId, int limit);
}
