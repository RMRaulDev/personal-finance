package com.rauldev.personalfinance.application;

public class ApplicationConstants {
    public static final int MAX_PAGE_SIZE = 100;
    public static final String ACCOUNT_NAME_ALREADY_EXISTS_MESSAGE =
        "An account with the same name already exists for this user";
    public static final String CATEGORY_NAME_ALREADY_EXISTS_MESSAGE =
        "A category with the same name already exists for this user";
    public static final String OBLIGATION_NAME_ALREADY_EXISTS_MESSAGE =
        "An obligation with the same name already exists for this user";
    public static final String OCCURRENCE_ALREADY_RESOLVED_MESSAGE =
        "The occurrence is already resolved";

    private ApplicationConstants() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
}
