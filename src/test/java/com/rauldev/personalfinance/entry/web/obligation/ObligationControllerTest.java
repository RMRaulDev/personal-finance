package com.rauldev.personalfinance.entry.web.obligation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class ObligationControllerTest {

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
        database = new SqliteTestDatabase(tempDir.resolve("obligations.db"));
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

    private static final String OBLIGATIONS = "/api/v1/obligations";

    private void seedObligation(String frequency, String start, String end, String status) {
        database.insertObligation(OBLIGATION_ID, USER_ID, "Rent", 2500, ACCOUNT_ID, CATEGORY_ID, frequency, start, end,
            status);
    }

    private static String createBody(String name, Object amountCents, Object accountId, Object categoryId,
                                     String recurrence) {
        StringBuilder json = new StringBuilder("{");
        appendField(json, "name", name == null ? null : "\"" + name + "\"");
        appendField(json, "amountCents", amountCents);
        appendField(json, "accountId", accountId == null ? null : "\"" + accountId + "\"");
        appendField(json, "categoryId", categoryId == null ? null : "\"" + categoryId + "\"");
        appendField(json, "recurrence", recurrence);
        return json.append("}").toString();
    }

    private static void appendField(StringBuilder json, String field, Object value) {
        if (value != null) {
            if (json.length() > 1) {
                json.append(",");
            }
            json.append("\"").append(field).append("\":").append(value);
        }
    }

    private static String monthly() {
        return "{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-20\"}";
    }

    private static String validBody() {
        return createBody("Rent", 2500, ACCOUNT_ID, CATEGORY_ID, monthly());
    }

    // ---------- create ----------

    @Test
    void createsAnObligationAnswers201WithIdLocationAndPersistedRow() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS, createBody("Rent", 2500, ACCOUNT_ID, CATEGORY_ID,
            "{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-20\",\"endDate\":\"2026-12-20\"}"));

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("http://localhost:" + port + OBLIGATIONS + "/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(200, send("GET", OBLIGATIONS + "/" + id, null).statusCode());
        assertEquals(2, database.count("obligations"));
        assertEquals("Rent", database.queryFirstColumn("SELECT name FROM obligations WHERE id = ?", id));
        assertEquals("2500", database.queryFirstColumn("SELECT amount FROM obligations WHERE id = ?", id));
        assertEquals(ACCOUNT_ID.toString(),
            database.queryFirstColumn("SELECT account_id FROM obligations WHERE id = ?", id));
        assertEquals(CATEGORY_ID.toString(),
            database.queryFirstColumn("SELECT category_id FROM obligations WHERE id = ?", id));
        assertEquals("MONTHLY", database.queryFirstColumn("SELECT frequency FROM obligations WHERE id = ?", id));
        assertEquals("2026-10-20", database.queryFirstColumn("SELECT start_date FROM obligations WHERE id = ?", id));
        assertEquals("2026-12-20", database.queryFirstColumn("SELECT end_date FROM obligations WHERE id = ?", id));
        assertEquals("ACTIVE", database.queryFirstColumn("SELECT status FROM obligations WHERE id = ?", id));
        assertEquals(USER_ID.toString(), database.queryFirstColumn("SELECT user_id FROM obligations WHERE id = ?", id));
    }

    @Test
    void createsAnObligationWithoutEndDate() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS, validBody());

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertNull(database.queryFirstColumn("SELECT end_date FROM obligations WHERE id = ?", id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "amountCents", "accountId", "categoryId", "recurrence", "recurrence.frequency",
        "recurrence.startDate"})
    void createWithoutARequiredFieldAnswers400NamingItAndPersistsNothing(String missing) throws Exception {
        String recurrence = "{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-20\"}";
        if (missing.equals("recurrence.frequency")) {
            recurrence = "{\"startDate\":\"2026-10-20\"}";
        } else if (missing.equals("recurrence.startDate")) {
            recurrence = "{\"frequency\":\"MONTHLY\"}";
        }
        String body = createBody(
            missing.equals("name") ? null : "Rent",
            missing.equals("amountCents") ? null : 2500,
            missing.equals("accountId") ? null : ACCOUNT_ID,
            missing.equals("categoryId") ? null : CATEGORY_ID,
            missing.equals("recurrence") ? null : recurrence);

        HttpResponse<String> response = send("POST", OBLIGATIONS, body);

        assertProblem(response, 400, "Field '" + missing + "' is required");
        assertEquals(1, database.count("obligations"));
    }

    @Test
    void createWithInvalidFrequencyAnswers400() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS, createBody("Rent", 2500, ACCOUNT_ID, CATEGORY_ID,
            "{\"frequency\":\"DAILY\",\"startDate\":\"2026-10-20\"}"));

        assertEquals(400, response.statusCode());
        assertEquals(1, database.count("obligations"));
    }

    @Test
    void createWithDuplicateNameAnswers409() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("POST", OBLIGATIONS, validBody());

        assertConflict(response, "OBLIGATION_NAME_ALREADY_EXISTS");
        assertEquals(2, database.count("obligations"));
    }

    @Test
    void createWithStartDateMoreThan31DaysInThePastAnswers400() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS, createBody("Rent", 2500, ACCOUNT_ID, CATEGORY_ID,
            "{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-09-03\"}"));

        assertEquals(400, response.statusCode());
    }

    @Test
    void createWithStartDateExactly31DaysInThePastIsAccepted() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS, createBody("Rent", 2500, ACCOUNT_ID, CATEGORY_ID,
            "{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-09-04\"}"));

        assertEquals(201, response.statusCode());
    }

    @Test
    void createWithAnotherUsersAccountAnswers404() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS,
            createBody("Rent", 2500, OTHER_ACCOUNT_ID, CATEGORY_ID, monthly()));

        assertEquals(404, response.statusCode());
        assertEquals(1, database.count("obligations"));
    }

    @Test
    void createWithAnotherUsersCategoryAnswers404() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS,
            createBody("Rent", 2500, ACCOUNT_ID, OTHER_CATEGORY_ID, monthly()));

        assertEquals(404, response.statusCode());
        assertEquals(1, database.count("obligations"));
    }

    // ---------- modify ----------

    @Test
    void patchingOnlyTheNameKeepsEveryOtherField() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", "2026-12-20", "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID, "{\"name\":\"Mortgage\"}");

        assertEquals(204, response.statusCode());
        assertEquals("Mortgage", obligationColumn("name"));
        assertEquals("2500", obligationColumn("amount"));
        assertEquals(ACCOUNT_ID.toString(), obligationColumn("account_id"));
        assertEquals(CATEGORY_ID.toString(), obligationColumn("category_id"));
        assertEquals("MONTHLY", obligationColumn("frequency"));
        assertEquals("2026-10-20", obligationColumn("start_date"));
        assertEquals("2026-12-20", obligationColumn("end_date"));
    }

    @Test
    void patchingAmountAndPaymentSourceChangesThem() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"amountCents\":3000,\"accountId\":\"" + SECOND_ACCOUNT_ID + "\",\"categoryId\":\""
                + SECOND_CATEGORY_ID + "\"}");

        assertEquals(204, response.statusCode());
        assertEquals("3000", obligationColumn("amount"));
        assertEquals(SECOND_ACCOUNT_ID.toString(), obligationColumn("account_id"));
        assertEquals(SECOND_CATEGORY_ID.toString(), obligationColumn("category_id"));
    }

    @Test
    void patchingOnlyTheAccountKeepsTheCategory() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"accountId\":\"" + SECOND_ACCOUNT_ID + "\"}");

        assertEquals(204, response.statusCode());
        assertEquals(SECOND_ACCOUNT_ID.toString(), obligationColumn("account_id"));
        assertEquals(CATEGORY_ID.toString(), obligationColumn("category_id"));
    }

    @Test
    void patchingOnlyTheCategoryKeepsTheAccount() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"categoryId\":\"" + SECOND_CATEGORY_ID + "\"}");

        assertEquals(204, response.statusCode());
        assertEquals(ACCOUNT_ID.toString(), obligationColumn("account_id"));
        assertEquals(SECOND_CATEGORY_ID.toString(), obligationColumn("category_id"));
    }

    @Test
    void patchingWithAnotherUsersAccountAnswers404AndKeepsTheSource() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"accountId\":\"" + OTHER_ACCOUNT_ID + "\"}");

        assertEquals(404, response.statusCode());
        assertEquals(ACCOUNT_ID.toString(), obligationColumn("account_id"));
        assertEquals(CATEGORY_ID.toString(), obligationColumn("category_id"));
    }

    @Test
    void patchingRecurrenceReplacesTheCalendar() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", "2026-12-20", "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"recurrence\":{\"frequency\":\"WEEKLY\",\"startDate\":\"2026-10-10\",\"endDate\":\"2026-11-10\"}}");

        assertEquals(204, response.statusCode());
        assertEquals("WEEKLY", obligationColumn("frequency"));
        assertEquals("2026-10-10", obligationColumn("start_date"));
        assertEquals("2026-11-10", obligationColumn("end_date"));
    }

    @Test
    void patchingRecurrenceWithoutEndDateRemovesTheEnd() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", "2026-12-20", "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"recurrence\":{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-20\"}}");

        assertEquals(204, response.statusCode());
        assertNull(obligationColumn("end_date"));
    }

    @Test
    void patchingRecurrenceWithNullEndDateRemovesTheEnd() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", "2026-12-20", "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"recurrence\":{\"frequency\":\"MONTHLY\",\"startDate\":\"2026-10-20\",\"endDate\":null}}");

        assertEquals(204, response.statusCode());
        assertNull(obligationColumn("end_date"));
    }

    @Test
    void patchingWithAnEmptyObjectAnswers400() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID, "{}");

        assertProblem(response, 400, "At least one change is required");
    }

    @Test
    void patchingWithoutABodyAnswers400() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID, null);

        assertEquals(400, response.statusCode());
    }

    @Test
    void patchingAnIncompleteRecurrenceAnswers400NamingTheField() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID,
            "{\"recurrence\":{\"startDate\":\"2026-10-20\"}}");

        assertProblem(response, 400, "Field 'recurrence.frequency' is required");
    }

    @Test
    void patchingANegativeAmountAnswers400AndKeepsTheAmount() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID, "{\"amountCents\":-1}");

        assertEquals(400, response.statusCode());
        assertEquals("2500", obligationColumn("amount"));
    }

    @Test
    void patchingAnArchivedObligationAnswers409() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ARCHIVED");

        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OBLIGATION_ID, "{\"name\":\"Mortgage\"}");

        assertConflict(response, "OBLIGATION_ARCHIVED");
        assertEquals("Rent", obligationColumn("name"));
    }

    @Test
    void patchingAnotherUsersObligationAnswers404AndKeepsIt() throws Exception {
        HttpResponse<String> response = send("PATCH", OBLIGATIONS + "/" + OTHER_OBLIGATION_ID,
            "{\"name\":\"Hijacked\"}");

        assertEquals(404, response.statusCode());
        assertEquals("Theirs", database.queryFirstColumn("SELECT name FROM obligations WHERE id = ?",
            OTHER_OBLIGATION_ID.toString()));
    }

    // ---------- archive ----------

    @Test
    void archivesAnObligationAnswers204AndStoresArchivedStatus() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("POST", OBLIGATIONS + "/" + OBLIGATION_ID + "/archive", null);

        assertEquals(204, response.statusCode());
        assertEquals("ARCHIVED", obligationColumn("status"));
    }

    @Test
    void archivingAnObligationWithOverdueOccurrencesAnswers409AndKeepsItActive() throws Exception {
        seedObligation("WEEKLY", "2026-09-01", null, "ACTIVE");

        HttpResponse<String> response = send("POST", OBLIGATIONS + "/" + OBLIGATION_ID + "/archive", null);

        assertConflict(response, "OBLIGATION_HAS_OVERDUE_OCCURRENCES");
        assertEquals("ACTIVE", obligationColumn("status"));
    }

    @Test
    void archivingAnotherUsersObligationAnswers404AndKeepsItActive() throws Exception {
        HttpResponse<String> response = send("POST", OBLIGATIONS + "/" + OTHER_OBLIGATION_ID + "/archive", null);

        assertEquals(404, response.statusCode());
        assertEquals("ACTIVE", database.queryFirstColumn("SELECT status FROM obligations WHERE id = ?",
            OTHER_OBLIGATION_ID.toString()));
    }

    // ---------- list and get ----------

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.propertyNames().forEach(names::add);
        return names;
    }

    @Test
    void listsObligationsAsBareArrayOrderedByNameIncludingArchived() throws Exception {
        UUID zetaId = UUID.fromString("40000000-0000-4000-8000-000000000011");
        database.insertObligation(zetaId, USER_ID, "Zeta", 100, ACCOUNT_ID, CATEGORY_ID, "WEEKLY", "2026-09-01",
            null, "ARCHIVED");
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");
        database.insertObligation(UUID.fromString("40000000-0000-4000-8000-000000000012"), USER_ID, "alpha", 100,
            ACCOUNT_ID, CATEGORY_ID, "WEEKLY", "2026-09-01", null, "ACTIVE");

        HttpResponse<String> response = send("GET", OBLIGATIONS, null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertTrue(body.isArray());
        assertEquals(3, body.size());
        assertEquals("Rent", body.get(0).path("name").asString());
        assertEquals("Zeta", body.get(1).path("name").asString());
        assertEquals("ARCHIVED", body.get(1).path("status").asString());
        assertEquals("alpha", body.get(2).path("name").asString());
    }

    @Test
    void listElementHasExactlyTheDocumentedKeysWithNullEndDatePresent() throws Exception {
        seedObligation("MONTHLY", "2026-10-20", null, "ACTIVE");

        HttpResponse<String> response = send("GET", OBLIGATIONS, null);

        JsonNode item = mapper.readTree(response.body()).get(0);
        assertEquals(List.of("id", "name", "amountCents", "account", "category", "recurrence", "status"),
            fieldNames(item));
        assertEquals(OBLIGATION_ID.toString(), item.path("id").asString());
        assertEquals(2500, item.path("amountCents").asLong());
        assertEquals(List.of("id", "name"), fieldNames(item.path("account")));
        assertEquals("Wallet", item.path("account").path("name").asString());
        assertEquals(List.of("id", "name"), fieldNames(item.path("category")));
        assertEquals("Rent", item.path("category").path("name").asString());
        JsonNode recurrence = item.path("recurrence");
        assertEquals(List.of("frequency", "startDate", "endDate"), fieldNames(recurrence));
        assertEquals("MONTHLY", recurrence.path("frequency").asString());
        assertEquals("2026-10-20", recurrence.path("startDate").asString());
        assertTrue(recurrence.get("endDate").isNull());
        assertEquals("ACTIVE", item.path("status").asString());
    }

    @Test
    void listWithoutObligationsAnswersEmptyArray() throws Exception {
        database.reset();
        database.insertUser(USER_ID);

        HttpResponse<String> response = send("GET", OBLIGATIONS, null);

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void getsAnObligationWithTheExactShape() throws Exception {
        seedObligation("WEEKLY", "2026-10-01", "2026-12-01", "ARCHIVED");

        HttpResponse<String> response = send("GET", OBLIGATIONS + "/" + OBLIGATION_ID, null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(List.of("id", "name", "amountCents", "account", "category", "recurrence", "status"),
            fieldNames(body));
        assertEquals(OBLIGATION_ID.toString(), body.path("id").asString());
        assertEquals("Rent", body.path("name").asString());
        assertEquals(2500, body.path("amountCents").asLong());
        assertEquals(ACCOUNT_ID.toString(), body.path("account").path("id").asString());
        assertEquals("Wallet", body.path("account").path("name").asString());
        assertEquals(CATEGORY_ID.toString(), body.path("category").path("id").asString());
        assertEquals("Rent", body.path("category").path("name").asString());
        assertEquals("WEEKLY", body.path("recurrence").path("frequency").asString());
        assertEquals("2026-10-01", body.path("recurrence").path("startDate").asString());
        assertEquals("2026-12-01", body.path("recurrence").path("endDate").asString());
        assertEquals("ARCHIVED", body.path("status").asString());
    }

    @Test
    void getAnotherUsersObligationAnswers404() throws Exception {
        HttpResponse<String> response = send("GET", OBLIGATIONS + "/" + OTHER_OBLIGATION_ID, null);

        assertProblem(response, 404, "Obligation not found for user: " + OTHER_OBLIGATION_ID);
    }

    @Test
    void getUnknownObligationAnswers404() throws Exception {
        UUID unknown = UUID.fromString("40000000-0000-4000-8000-0000000000ff");

        HttpResponse<String> response = send("GET", OBLIGATIONS + "/" + unknown, null);

        assertProblem(response, 404, "Obligation not found for user: " + unknown);
    }

    @Test
    void getWithMalformedIdAnswers400() throws Exception {
        HttpResponse<String> response = send("GET", OBLIGATIONS + "/not-a-uuid", null);

        assertEquals(400, response.statusCode());
    }

    private String obligationColumn(String column) {
        return database.queryFirstColumn("SELECT " + column + " FROM obligations WHERE id = ?",
            OBLIGATION_ID.toString());
    }
}
