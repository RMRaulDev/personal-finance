package com.rauldev.personalfinance.entry.web.dashboard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.rauldev.personalfinance.entry.SqliteTestDatabase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardControllerTest {

    /** 2026-10-06T03:30Z is 2026-10-05 21:30 in Mexico City (UTC-6), so "today" is 2026-10-05, not the UTC date. */
    private static final Instant NOW = Instant.parse("2026-10-06T03:30:00Z");

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private static final UUID WALLET_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SAVINGS_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID OLD_CARD_ID = UUID.fromString("10000000-0000-4000-8000-000000000003");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000004");
    private static final UUID OTHER_ACCOUNT_2_ID = UUID.fromString("10000000-0000-4000-8000-000000000005");

    private static final UUID EXPENSE_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID INACTIVE_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID INCOME_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000004");

    private static final UUID RENT_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID GYM_ID = UUID.fromString("40000000-0000-4000-8000-000000000002");
    private static final UUID PHONE_ID = UUID.fromString("40000000-0000-4000-8000-000000000003");
    private static final UUID INSURANCE_ID = UUID.fromString("40000000-0000-4000-8000-000000000004");
    private static final UUID OTHER_OBLIGATION_ID = UUID.fromString("40000000-0000-4000-8000-000000000009");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @TempDir
    static Path tempDir;

    static SqliteTestDatabase database;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        // Boot forbids overriding the bean named "clock", so this one has another name and wins as @Primary.
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneId.of("America/Mexico_City"));
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        database = new SqliteTestDatabase(tempDir.resolve("dashboard.db"));
        registry.add("personal-finance.sqlite.url", database::url);
        registry.add("personal-finance.single-user-id", USER_ID::toString);
    }

    @LocalServerPort
    private int port;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void resetDatabase() {
        database.reset();
        database.insertUser(USER_ID);
        database.insertUser(OTHER_USER_ID);
        seedOtherUserData();
    }

    /** Large, overdue and recent data of another user: none of it may ever show up for the configured user. */
    private void seedOtherUserData() {
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Theirs", 900000, "ACTIVE");
        database.insertAccount(OTHER_ACCOUNT_2_ID, OTHER_USER_ID, "Theirs 2", 1000, "ACTIVE");
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Their category", "EXPENSE", "ACTIVE");
        database.insertObligation(OTHER_OBLIGATION_ID, OTHER_USER_ID, "Their rent", 777700, OTHER_ACCOUNT_ID,
            OTHER_CATEGORY_ID, "MONTHLY", "2026-08-20", null, "ACTIVE");
        database.insertIncome(UUID.fromString("50000000-0000-4000-8000-0000000000a1"), OTHER_ACCOUNT_ID,
            OTHER_CATEGORY_ID, 4242, "2026-10-04", "ACTIVE");
        database.insertTransfer(UUID.fromString("50000000-0000-4000-8000-0000000000a2"), OTHER_ACCOUNT_ID,
            OTHER_ACCOUNT_2_ID, 4343, "2026-10-04");
    }

    private void seedUserAccountsAndCategories() {
        database.insertAccount(WALLET_ID, USER_ID, "Wallet", 2000, "ACTIVE");
        database.insertAccount(SAVINGS_ID, USER_ID, "Savings", 500, "ACTIVE");
        database.insertAccount(OLD_CARD_ID, USER_ID, "Old card", 9999, "INACTIVE");
        database.insertCategory(EXPENSE_CATEGORY_ID, USER_ID, "Bills", "EXPENSE", "ACTIVE");
        database.insertCategory(INACTIVE_CATEGORY_ID, USER_ID, "Closed", "EXPENSE", "INACTIVE");
        database.insertCategory(INCOME_CATEGORY_ID, USER_ID, "Salary", "INCOME", "ACTIVE");
    }

    /**
     * Today is 2026-10-05, the horizon is 2026-10-05..2026-10-18.
     * Rent 3000 (Wallet) monthly from 09-20: 1 overdue. Gym 1000 (inactive Old card) on 10-10.
     * Phone 2000 (Savings) on 10-12. Committed 6000 against active balance 2500 (Old card does not count).
     */
    private void seedAttentionScenario() {
        seedUserAccountsAndCategories();
        database.insertObligation(RENT_ID, USER_ID, "Rent", 3000, WALLET_ID, EXPENSE_CATEGORY_ID, "MONTHLY",
            "2026-09-20", null, "ACTIVE");
        database.insertObligation(GYM_ID, USER_ID, "Gym", 1000, OLD_CARD_ID, EXPENSE_CATEGORY_ID, "MONTHLY",
            "2026-10-10", null, "ACTIVE");
        database.insertObligation(PHONE_ID, USER_ID, "Phone", 2000, SAVINGS_ID, EXPENSE_CATEGORY_ID, "MONTHLY",
            "2026-10-12", null, "ACTIVE");
    }

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode getDashboard() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/dashboard");
        assertEquals(200, response.statusCode());
        return mapper.readTree(response.body());
    }

    private static Set<String> keys(JsonNode node) {
        return Set.copyOf(node.propertyNames());
    }

    private static List<String> texts(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(item -> values.add(item.path(field).asString()));
        return values;
    }

    private static Set<String> keySet(String... keys) {
        return Set.of(keys);
    }

    // ---------- shape ----------

    @Test
    void answers200WithExactlyTheDocumentedTopLevelKeys() throws Exception {
        seedAttentionScenario();

        HttpResponse<String> response = send("GET", "/api/v1/dashboard");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(keySet("horizon", "availableToSpend", "attention", "upcomingCommitments", "recent"),
            keys(mapper.readTree(response.body())));
    }

    @Test
    void horizonStartsOnTheLocalDateOfTheClockZoneAndEndsThirteenDaysLater() throws Exception {
        JsonNode horizon = getDashboard().path("horizon");

        assertEquals(keySet("from", "to"), keys(horizon));
        assertEquals("2026-10-05", horizon.path("from").asString());
        assertEquals("2026-10-18", horizon.path("to").asString());
    }

    @Test
    void availableToSpendIsInCentsAndConsistentWithAShortfall() throws Exception {
        seedAttentionScenario();

        JsonNode available = getDashboard().path("availableToSpend");

        assertEquals(keySet("balanceCents", "committedCents", "availableCents", "shortfallCents"), keys(available));
        assertEquals(2500, available.path("balanceCents").asLong());
        assertEquals(6000, available.path("committedCents").asLong());
        assertEquals(0, available.path("availableCents").asLong());
        assertEquals(3500, available.path("shortfallCents").asLong());
    }

    @Test
    void availableToSpendReportsAvailableAndNoShortfallWhenBalanceCoversTheCommitments() throws Exception {
        seedUserAccountsAndCategories();
        database.insertObligation(RENT_ID, USER_ID, "Rent", 1000, WALLET_ID, EXPENSE_CATEGORY_ID, "ONCE",
            "2026-10-10", null, "ACTIVE");

        JsonNode available = getDashboard().path("availableToSpend");

        assertEquals(2500, available.path("balanceCents").asLong());
        assertEquals(1000, available.path("committedCents").asLong());
        assertEquals(1500, available.path("availableCents").asLong());
        assertEquals(0, available.path("shortfallCents").asLong());
    }

    // ---------- attention ----------

    @Test
    void attentionListsEveryTypeInCorePriorityOrder() throws Exception {
        seedAttentionScenario();

        JsonNode attention = getDashboard().path("attention");

        assertEquals(List.of("OVERDUE_OCCURRENCE", "SHORTFALL", "PAYMENT_BLOCKED", "ACCOUNT_SHORTFALL",
            "ACCOUNT_SHORTFALL"), texts(attention, "type"));
    }

    @Test
    void overdueOccurrenceItemHasItsOwnFields() throws Exception {
        seedAttentionScenario();

        JsonNode item = getDashboard().path("attention").get(0);

        assertEquals(keySet("type", "obligation", "oldestOverdue", "overdueCount", "overdueAmountCents"), keys(item));
        assertEquals(keySet("id", "name"), keys(item.path("obligation")));
        assertEquals(RENT_ID.toString(), item.path("obligation").path("id").asString());
        assertEquals("Rent", item.path("obligation").path("name").asString());
        assertEquals("2026-09-20", item.path("oldestOverdue").asString());
        assertEquals(1, item.path("overdueCount").asLong());
        assertEquals(3000, item.path("overdueAmountCents").asLong());
    }

    @Test
    void shortfallItemHasItsOwnFields() throws Exception {
        seedAttentionScenario();

        JsonNode item = getDashboard().path("attention").get(1);

        assertEquals(keySet("type", "shortfallCents"), keys(item));
        assertEquals(3500, item.path("shortfallCents").asLong());
    }

    @Test
    void paymentBlockedItemHasItsOwnFieldsAndFlagsTheInactiveAccount() throws Exception {
        seedAttentionScenario();

        JsonNode item = getDashboard().path("attention").get(2);

        assertEquals(keySet("type", "obligation", "account", "nearestDueDate", "committedCents", "accountInactive",
            "categoryInactive"), keys(item));
        assertEquals(GYM_ID.toString(), item.path("obligation").path("id").asString());
        assertEquals("Gym", item.path("obligation").path("name").asString());
        assertEquals(OLD_CARD_ID.toString(), item.path("account").path("id").asString());
        assertEquals("Old card", item.path("account").path("name").asString());
        assertEquals("2026-10-10", item.path("nearestDueDate").asString());
        assertEquals(1000, item.path("committedCents").asLong());
        assertTrue(item.path("accountInactive").asBoolean());
        assertFalse(item.path("categoryInactive").asBoolean());
    }

    @Test
    void paymentBlockedItemFlagsTheInactiveCategory() throws Exception {
        seedUserAccountsAndCategories();
        database.insertObligation(GYM_ID, USER_ID, "Gym", 1000, WALLET_ID, INACTIVE_CATEGORY_ID, "ONCE",
            "2026-10-10", null, "ACTIVE");

        JsonNode item = getDashboard().path("attention").get(0);

        assertEquals("PAYMENT_BLOCKED", item.path("type").asString());
        assertFalse(item.path("accountInactive").asBoolean());
        assertTrue(item.path("categoryInactive").asBoolean());
    }

    @Test
    void accountShortfallItemsHaveTheirOwnFieldsAndTheLargerShortfallComesFirst() throws Exception {
        seedAttentionScenario();

        JsonNode attention = getDashboard().path("attention");
        JsonNode first = attention.get(3);
        JsonNode second = attention.get(4);

        assertEquals(keySet("type", "account", "shortfallCents", "nearestDueDate"), keys(first));
        assertEquals(SAVINGS_ID.toString(), first.path("account").path("id").asString());
        assertEquals("Savings", first.path("account").path("name").asString());
        assertEquals(1500, first.path("shortfallCents").asLong());
        assertEquals("2026-10-12", first.path("nearestDueDate").asString());
        assertEquals(WALLET_ID.toString(), second.path("account").path("id").asString());
        assertEquals(1000, second.path("shortfallCents").asLong());
        assertEquals("2026-09-20", second.path("nearestDueDate").asString());
    }

    // ---------- upcoming commitments ----------

    @Test
    void upcomingCommitmentsAreOrderedByDueDateAndFlagBlockedPayments() throws Exception {
        seedAttentionScenario();

        JsonNode upcoming = getDashboard().path("upcomingCommitments");

        assertEquals(2, upcoming.size());
        JsonNode gym = upcoming.get(0);
        assertEquals(keySet("obligation", "dueDate", "amountCents", "account", "paymentBlocked"), keys(gym));
        assertEquals(GYM_ID.toString(), gym.path("obligation").path("id").asString());
        assertEquals("2026-10-10", gym.path("dueDate").asString());
        assertEquals(1000, gym.path("amountCents").asLong());
        assertEquals("Old card", gym.path("account").path("name").asString());
        assertTrue(gym.path("paymentBlocked").asBoolean());
        JsonNode phone = upcoming.get(1);
        assertEquals(PHONE_ID.toString(), phone.path("obligation").path("id").asString());
        assertEquals("2026-10-12", phone.path("dueDate").asString());
        assertEquals(2000, phone.path("amountCents").asLong());
        assertEquals("Savings", phone.path("account").path("name").asString());
        assertFalse(phone.path("paymentBlocked").asBoolean());
    }

    @Test
    void upcomingCommitmentsAreLimitedToFiveOrderedByDateThenName() throws Exception {
        database.insertAccount(WALLET_ID, USER_ID, "Wallet", 100000, "ACTIVE");
        database.insertCategory(EXPENSE_CATEGORY_ID, USER_ID, "Bills", "EXPENSE", "ACTIVE");
        // Inserted out of name order (C, A, B) so the name tie-break is what orders them.
        String[] names = {"C", "A", "B"};
        for (int i = 0; i < names.length; i++) {
            database.insertObligation(UUID.fromString("40000000-0000-4000-8000-00000000010" + i), USER_ID, names[i],
                100, WALLET_ID, EXPENSE_CATEGORY_ID, "WEEKLY", "2026-10-05", null, "ACTIVE");
        }

        JsonNode upcoming = getDashboard().path("upcomingCommitments");

        assertEquals(5, upcoming.size());
        assertEquals(List.of("A", "B", "C", "A", "B"),
            upcoming.valueStream().map(item -> item.path("obligation").path("name").asString()).toList());
        assertEquals(List.of("2026-10-05", "2026-10-05", "2026-10-05", "2026-10-12", "2026-10-12"),
            texts(upcoming, "dueDate"));
    }

    @Test
    void upcomingCommitmentsIncludeTheLastHorizonDayAndExcludeTheNextOne() throws Exception {
        database.insertAccount(WALLET_ID, USER_ID, "Wallet", 100000, "ACTIVE");
        database.insertCategory(EXPENSE_CATEGORY_ID, USER_ID, "Bills", "EXPENSE", "ACTIVE");
        database.insertObligation(UUID.fromString("40000000-0000-4000-8000-000000000201"), USER_ID, "Last day", 100, WALLET_ID, EXPENSE_CATEGORY_ID,
            "ONCE", "2026-10-18", null, "ACTIVE");
        database.insertObligation(UUID.fromString("40000000-0000-4000-8000-000000000202"), USER_ID, "After horizon", 100, WALLET_ID, EXPENSE_CATEGORY_ID,
            "ONCE", "2026-10-19", null, "ACTIVE");

        JsonNode upcoming = getDashboard().path("upcomingCommitments");

        assertEquals(1, upcoming.size());
        assertEquals("Last day", upcoming.get(0).path("obligation").path("name").asString());
    }

    // ---------- recent activity ----------

    private void seedRecentActivity() {
        seedUserAccountsAndCategories();
        seedRecentOperations();
    }

    /**
     * Needs the accounts and categories. Newest first: income 10-04, expense paying Insurance 10-03, transfer 10-02,
     * plain expense 10-01 (plus a cancelled income that must not appear).
     */
    private void seedRecentOperations() {
        UUID paidExpense = UUID.fromString("50000000-0000-4000-8000-000000000002");
        database.insertIncome(UUID.fromString("50000000-0000-4000-8000-000000000001"), WALLET_ID, INCOME_CATEGORY_ID,
            120000, "2026-10-04", "ACTIVE");
        database.insertExpense(paidExpense, WALLET_ID, EXPENSE_CATEGORY_ID, 2500, "2026-10-03", "ACTIVE");
        database.insertTransfer(UUID.fromString("50000000-0000-4000-8000-000000000003"), WALLET_ID, SAVINGS_ID, 700,
            "2026-10-02");
        database.insertExpense(UUID.fromString("50000000-0000-4000-8000-000000000004"), WALLET_ID,
            EXPENSE_CATEGORY_ID, 300, "2026-10-01", "ACTIVE");
        database.insertIncome(UUID.fromString("50000000-0000-4000-8000-000000000005"), WALLET_ID, INCOME_CATEGORY_ID,
            999, "2026-10-04", "CANCELLED");
        database.insertObligation(INSURANCE_ID, USER_ID, "Insurance", 2500, WALLET_ID, EXPENSE_CATEGORY_ID, "ONCE",
            "2026-10-03", null, "ACTIVE");
        database.insertResolution(UUID.fromString("60000000-0000-4000-8000-000000000001"), INSURANCE_ID, "2026-10-03",
            "PAID", paidExpense);
    }

    @Test
    void recentActivityIsNewestFirstAndExcludesCancelledOperations() throws Exception {
        seedRecentActivity();

        JsonNode recent = getDashboard().path("recent");

        assertEquals(List.of("INCOME", "EXPENSE", "TRANSFER", "EXPENSE"), texts(recent, "type"));
        assertEquals(List.of("2026-10-04", "2026-10-03", "2026-10-02", "2026-10-01"), texts(recent, "operationDate"));
    }

    @Test
    void recentIncomeHasAccountAndCategoryAndExplicitNullTransferAndObligation() throws Exception {
        seedRecentActivity();

        JsonNode income = getDashboard().path("recent").get(0);

        assertEquals(keySet("id", "type", "amountCents", "operationDate", "account", "category", "transfer",
            "obligation"), keys(income));
        assertEquals("50000000-0000-4000-8000-000000000001", income.path("id").asString());
        assertEquals(120000, income.path("amountCents").asLong());
        assertEquals("Wallet", income.path("account").path("name").asString());
        assertEquals("Salary", income.path("category").path("name").asString());
        assertTrue(income.get("transfer").isNull());
        assertTrue(income.get("obligation").isNull());
    }

    @Test
    void recentExpenseThatPaidAnOccurrenceCarriesTheObligation() throws Exception {
        seedRecentActivity();

        JsonNode expense = getDashboard().path("recent").get(1);

        assertEquals(2500, expense.path("amountCents").asLong());
        assertEquals(INSURANCE_ID.toString(), expense.path("obligation").path("id").asString());
        assertEquals("Insurance", expense.path("obligation").path("name").asString());
        assertEquals("Bills", expense.path("category").path("name").asString());
    }

    @Test
    void recentExpenseWithoutAnObligationHasAnExplicitNullObligation() throws Exception {
        seedRecentActivity();

        JsonNode expense = getDashboard().path("recent").get(3);

        assertEquals("EXPENSE", expense.path("type").asString());
        assertTrue(expense.has("obligation"));
        assertTrue(expense.get("obligation").isNull());
    }

    @Test
    void recentTransferHasTransferAndExplicitNullAccountCategoryAndObligation() throws Exception {
        seedRecentActivity();

        JsonNode transfer = getDashboard().path("recent").get(2);

        assertEquals(keySet("id", "type", "amountCents", "operationDate", "account", "category", "transfer",
            "obligation"), keys(transfer));
        assertEquals(700, transfer.path("amountCents").asLong());
        assertTrue(transfer.get("account").isNull());
        assertTrue(transfer.get("category").isNull());
        assertTrue(transfer.get("obligation").isNull());
        assertEquals(keySet("sourceAccount", "targetAccount"), keys(transfer.path("transfer")));
        assertEquals(WALLET_ID.toString(), transfer.path("transfer").path("sourceAccount").path("id").asString());
        assertEquals("Wallet", transfer.path("transfer").path("sourceAccount").path("name").asString());
        assertEquals(SAVINGS_ID.toString(), transfer.path("transfer").path("targetAccount").path("id").asString());
        assertEquals("Savings", transfer.path("transfer").path("targetAccount").path("name").asString());
    }

    @Test
    void recentActivityIsLimitedToTheFiveNewestOperations() throws Exception {
        seedUserAccountsAndCategories();
        for (int day = 1; day <= 6; day++) {
            database.insertIncome(UUID.fromString("50000000-0000-4000-8000-00000000020" + day), WALLET_ID,
                INCOME_CATEGORY_ID, 100 * day, LocalDate.of(2026, 9, 29).plusDays(day).toString(), "ACTIVE");
        }

        JsonNode recent = getDashboard().path("recent");

        assertEquals(5, recent.size());
        assertEquals(List.of("2026-10-05", "2026-10-04", "2026-10-03", "2026-10-02", "2026-10-01"),
            texts(recent, "operationDate"));
    }

    // ---------- empty and isolation ----------

    @Test
    void aUserWithoutDataGetsZerosAndEmptyListsEvenWhenAnotherUserHasData() throws Exception {
        JsonNode body = getDashboard();

        assertEquals(keySet("horizon", "availableToSpend", "attention", "upcomingCommitments", "recent"), keys(body));
        assertEquals(0, body.path("availableToSpend").path("balanceCents").asLong());
        assertEquals(0, body.path("availableToSpend").path("committedCents").asLong());
        assertEquals(0, body.path("availableToSpend").path("availableCents").asLong());
        assertEquals(0, body.path("availableToSpend").path("shortfallCents").asLong());
        assertTrue(body.path("attention").isArray());
        assertEquals(0, body.path("attention").size());
        assertTrue(body.path("upcomingCommitments").isArray());
        assertEquals(0, body.path("upcomingCommitments").size());
        assertTrue(body.path("recent").isArray());
        assertEquals(0, body.path("recent").size());
    }

    @Test
    void anotherUsersDataNeverAppearsInTheUsersDashboard() throws Exception {
        seedAttentionScenario();
        seedRecentOperations();

        HttpResponse<String> response = send("GET", "/api/v1/dashboard");

        assertEquals(200, response.statusCode());
        String body = response.body();
        assertFalse(body.contains("Their rent"));
        assertFalse(body.contains("Theirs"));
        assertFalse(body.contains(OTHER_ACCOUNT_ID.toString()));
        JsonNode available = mapper.readTree(body).path("availableToSpend");
        assertEquals(2500, available.path("balanceCents").asLong());
        assertEquals(6000, available.path("committedCents").asLong());
    }

    // ---------- method ----------

    @Test
    void postingToTheDashboardAnswers405() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/dashboard");

        assertEquals(405, response.statusCode());
    }
}
