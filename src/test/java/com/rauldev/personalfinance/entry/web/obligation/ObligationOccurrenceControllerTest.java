package com.rauldev.personalfinance.entry.web.obligation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class ObligationOccurrenceControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T15:30:00Z");

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000003");
    private static final UUID CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
    private static final UUID OBLIGATION_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_OBLIGATION_ID = UUID.fromString("40000000-0000-4000-8000-000000000002");

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
        database = new SqliteTestDatabase(tempDir.resolve("occurrences.db"));
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
        database.insertAccount(ACCOUNT_ID, USER_ID, "Wallet", 10000, "ACTIVE");
        database.insertAccount(SECOND_ACCOUNT_ID, USER_ID, "Savings", 8000, "ACTIVE");
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Theirs", 10000, "ACTIVE");
        database.insertCategory(CATEGORY_ID, USER_ID, "Rent", "EXPENSE", "ACTIVE");
        database.insertCategory(SECOND_CATEGORY_ID, USER_ID, "Utilities", "EXPENSE", "ACTIVE");
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Their rent", "EXPENSE", "ACTIVE");
        database.insertObligation(OTHER_OBLIGATION_ID, OTHER_USER_ID, "Theirs", 2500, OTHER_ACCOUNT_ID,
            OTHER_CATEGORY_ID, "MONTHLY", "2026-08-20", null, "ACTIVE");
    }

    private String balance(UUID accountId) {
        return database.queryFirstColumn("SELECT balance FROM accounts WHERE id = ?", accountId.toString());
    }

    private void assertProblem(HttpResponse<String> response, int status, String detail) throws Exception {
        assertEquals(status, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals(detail, mapper.readTree(response.body()).path("detail").asString());
    }

    private void assertConflict(HttpResponse<String> response, String code) throws Exception {
        assertEquals(409, response.statusCode());
        assertEquals(code, mapper.readTree(response.body()).path("code").asString());
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "application/json");
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final String BASE = "/api/v1/obligations/" + OBLIGATION_ID;
    private static final String OTHER_BASE = "/api/v1/obligations/" + OTHER_OBLIGATION_ID;

    /** Monthly on the 15th from October: every date is pending or in the future relative to 2026-10-05. */
    private void seedUpcomingObligation() {
        database.insertObligation(OBLIGATION_ID, USER_ID, "Rent", 2500, ACCOUNT_ID, CATEGORY_ID, "MONTHLY",
            "2026-10-15", null, "ACTIVE");
    }

    /** Monthly on the 20th from August: 2026-08-20 and 2026-09-20 are overdue on 2026-10-05. */
    private void seedOverdueObligation() {
        database.insertObligation(OBLIGATION_ID, USER_ID, "Rent", 2500, ACCOUNT_ID, CATEGORY_ID, "MONTHLY",
            "2026-08-20", null, "ACTIVE");
    }

    private String expenseColumn(String column, String id) {
        return database.queryFirstColumn("SELECT " + column + " FROM expense_operations WHERE id = ?", id);
    }

    private String operationId(HttpResponse<String> response) throws Exception {
        return mapper.readTree(response.body()).path("operationId").asString();
    }

    private String resolutionStatus(String dueDate) {
        return database.queryFirstColumn(
            "SELECT status FROM occurrence_resolutions WHERE obligation_id = ? AND due_date = ?",
            OBLIGATION_ID.toString(), dueDate);
    }

    // ---------- pay ----------

    @Test
    void payingWithoutABodyUsesDefaultsAnswers201WithLocationAndStoresPaidResolution() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay", null);

        assertEquals(201, response.statusCode());
        String id = operationId(response);
        assertEquals("http://localhost:" + port + "/api/v1/operations/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(1, database.count("expense_operations"));
        assertEquals("2500", expenseColumn("amount", id));
        assertEquals(ACCOUNT_ID.toString(), expenseColumn("account_id", id));
        assertEquals(CATEGORY_ID.toString(), expenseColumn("category_id", id));
        assertEquals("2026-10-05", expenseColumn("operation_date", id));
        assertEquals("7500", balance(ACCOUNT_ID));
        assertEquals("PAID", resolutionStatus("2026-10-15"));
        assertEquals(id, database.queryFirstColumn(
            "SELECT expense_id FROM occurrence_resolutions WHERE obligation_id = ?", OBLIGATION_ID.toString()));
    }

    @Test
    void payingWithAnEmptyObjectUsesTheSameDefaults() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay", "{}");

        assertEquals(201, response.statusCode());
        String id = operationId(response);
        assertEquals("2500", expenseColumn("amount", id));
        assertEquals("2026-10-05", expenseColumn("operation_date", id));
        assertEquals("PAID", resolutionStatus("2026-10-15"));
    }

    @Test
    void payingWithOverridesAppliesThem() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay",
            "{\"amountCents\":1500,\"operationDate\":\"2026-10-03\",\"accountId\":\"" + SECOND_ACCOUNT_ID
                + "\",\"categoryId\":\"" + SECOND_CATEGORY_ID + "\"}");

        assertEquals(201, response.statusCode());
        String id = operationId(response);
        assertEquals("1500", expenseColumn("amount", id));
        assertEquals("2026-10-03", expenseColumn("operation_date", id));
        assertEquals(SECOND_ACCOUNT_ID.toString(), expenseColumn("account_id", id));
        assertEquals(SECOND_CATEGORY_ID.toString(), expenseColumn("category_id", id));
        assertEquals("6500", balance(SECOND_ACCOUNT_ID));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void payingWithZeroAmountAnswers400AndPersistsNothing() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay", "{\"amountCents\":0}");

        assertEquals(400, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
        assertEquals(0, database.count("occurrence_resolutions"));
    }

    @Test
    void payingWithAFutureOperationDateAnswers400AndPersistsNothing() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay",
            "{\"operationDate\":\"2026-10-06\"}");

        assertEquals(400, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void payingWithTodayAsOperationDateIsAccepted() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay",
            "{\"operationDate\":\"2026-10-05\"}");

        assertEquals(201, response.statusCode());
    }

    @Test
    void payingTheSameOccurrenceTwiceAnswers409AndDebitsOnlyOnce() throws Exception {
        seedUpcomingObligation();
        send("POST", BASE + "/occurrences/2026-10-15/pay", null);

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay", null);

        assertConflict(response, "OCCURRENCE_ALREADY_RESOLVED");
        assertEquals(1, database.count("expense_operations"));
        assertEquals("7500", balance(ACCOUNT_ID));
    }

    @Test
    void payingAgainAfterCancellingTheExpenseAnswers201() throws Exception {
        seedUpcomingObligation();
        String firstId = operationId(send("POST", BASE + "/occurrences/2026-10-15/pay", null));
        assertEquals(204, send("POST", "/api/v1/operations/" + firstId + "/cancel", null).statusCode());
        assertEquals(0, database.count("occurrence_resolutions"));

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay", null);

        assertEquals(201, response.statusCode());
        assertEquals("PAID", resolutionStatus("2026-10-15"));
        assertEquals(1, database.count("occurrence_resolutions"));
    }

    @Test
    void payingADateOffTheCalendarAnswers409() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-16/pay", null);

        assertConflict(response, "OCCURRENCE_NOT_SCHEDULED");
        assertEquals(0, database.count("expense_operations"));
    }

    @Test
    void payingWithAnInvalidDueDateAnswers400() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/not-a-date/pay", null);

        assertEquals(400, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
    }

    @Test
    void payingAnotherUsersObligationAnswers404WithoutSideEffects() throws Exception {
        HttpResponse<String> response = send("POST", OTHER_BASE + "/occurrences/2026-08-20/pay", null);

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
        assertEquals(0, database.count("occurrence_resolutions"));
        assertEquals("10000", balance(OTHER_ACCOUNT_ID));
    }

    @Test
    void payingWithAnotherUsersAccountAnswers404WithoutSideEffects() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/pay",
            "{\"accountId\":\"" + OTHER_ACCOUNT_ID + "\"}");

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
        assertEquals(0, database.count("occurrence_resolutions"));
        assertEquals("10000", balance(ACCOUNT_ID));
        assertEquals("10000", balance(OTHER_ACCOUNT_ID));
    }

    // ---------- skip ----------

    @Test
    void skippingAnOccurrenceAnswers204AndStoresSkippedResolution() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/skip", null);

        assertEquals(204, response.statusCode());
        assertEquals("SKIPPED", resolutionStatus("2026-10-15"));
        assertEquals(0, database.count("expense_operations"));
    }

    @Test
    void skippingAnotherUsersObligationAnswers404WithoutSideEffects() throws Exception {
        HttpResponse<String> response = send("POST", OTHER_BASE + "/occurrences/2026-08-20/skip", null);

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("occurrence_resolutions"));
    }

    // ---------- reopen ----------

    @Test
    void reopeningASkippedOccurrenceAnswers204AndRemovesTheResolution() throws Exception {
        seedUpcomingObligation();
        send("POST", BASE + "/occurrences/2026-10-15/skip", null);

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/reopen", null);

        assertEquals(204, response.statusCode());
        assertEquals(0, database.count("occurrence_resolutions"));
    }

    @Test
    void reopeningAPaidOccurrenceAnswers409AndKeepsThePayment() throws Exception {
        seedUpcomingObligation();
        send("POST", BASE + "/occurrences/2026-10-15/pay", null);

        HttpResponse<String> response = send("POST", BASE + "/occurrences/2026-10-15/reopen", null);

        assertConflict(response, "OCCURRENCE_PAID_NOT_REOPENABLE");
        assertEquals("PAID", resolutionStatus("2026-10-15"));
        assertEquals("7500", balance(ACCOUNT_ID));
    }

    @Test
    void reopeningAnotherUsersObligationAnswers404WithoutSideEffects() throws Exception {
        database.insertResolution(UUID.randomUUID(), OTHER_OBLIGATION_ID, "2026-08-20", "SKIPPED", null);

        HttpResponse<String> response = send("POST", OTHER_BASE + "/occurrences/2026-08-20/reopen", null);

        assertEquals(404, response.statusCode());
        assertEquals(1, database.count("occurrence_resolutions"));
    }

    // ---------- skip overdue ----------

    @Test
    void skippingOverdueOccurrencesAnswers200WithAscendingDates() throws Exception {
        seedOverdueObligation();

        HttpResponse<String> response = send("POST", BASE + "/overdue-occurrences/skip", null);

        assertEquals(200, response.statusCode());
        JsonNode dates = mapper.readTree(response.body()).path("skippedDueDates");
        assertEquals(2, dates.size());
        assertEquals("2026-08-20", dates.get(0).asString());
        assertEquals("2026-09-20", dates.get(1).asString());
        assertEquals("SKIPPED", resolutionStatus("2026-08-20"));
        assertEquals("SKIPPED", resolutionStatus("2026-09-20"));
        assertEquals(2, database.count("occurrence_resolutions"));
    }

    @Test
    void skippingOverdueOccurrencesWithNothingOverdueAnswersAnEmptyList() throws Exception {
        seedUpcomingObligation();

        HttpResponse<String> response = send("POST", BASE + "/overdue-occurrences/skip", null);

        assertEquals(200, response.statusCode());
        assertEquals(0, mapper.readTree(response.body()).path("skippedDueDates").size());
        assertEquals(0, database.count("occurrence_resolutions"));
    }

    @Test
    void skippingOverdueOccurrencesOfAnotherUsersObligationAnswers404WithoutSideEffects() throws Exception {
        HttpResponse<String> response = send("POST", OTHER_BASE + "/overdue-occurrences/skip", null);

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("occurrence_resolutions"));
    }
}
