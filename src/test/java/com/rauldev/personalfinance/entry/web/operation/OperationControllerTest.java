package com.rauldev.personalfinance.entry.web.operation;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class OperationControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T15:30:00Z");

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000003");
    private static final UUID INCOME_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID EXPENSE_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");

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
        database = new SqliteTestDatabase(tempDir.resolve("operations.db"));
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
        database.insertAccount(SECOND_ACCOUNT_ID, USER_ID, "Savings", 0, "ACTIVE");
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Theirs", 10000, "ACTIVE");
        database.insertCategory(INCOME_CATEGORY_ID, USER_ID, "Salary", "INCOME", "ACTIVE");
        database.insertCategory(EXPENSE_CATEGORY_ID, USER_ID, "Food", "EXPENSE", "ACTIVE");
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Their food", "EXPENSE", "ACTIVE");
    }

    // ---------- registrations ----------

    @Test
    void registersIncomeAnswers201WithLocationIdPersistedRowAndCreditedBalance() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/incomes",
            accountOperation(ACCOUNT_ID, INCOME_CATEGORY_ID, 5000, "2026-10-01"));

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("http://localhost:" + port + "/api/v1/operations/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(1, database.count("income_operations"));
        assertEquals(ACCOUNT_ID.toString(),
            database.queryFirstColumn("SELECT account_id FROM income_operations WHERE id = ?", id));
        assertEquals("5000", database.queryFirstColumn("SELECT amount FROM income_operations WHERE id = ?", id));
        assertEquals("2026-10-01",
            database.queryFirstColumn("SELECT operation_date FROM income_operations WHERE id = ?", id));
        assertEquals("15000", balance(ACCOUNT_ID));
    }

    @Test
    void registersExpenseAnswers201WithLocationIdPersistedRowAndDebitedBalance() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, EXPENSE_CATEGORY_ID, 2500, "2026-10-02"));

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("http://localhost:" + port + "/api/v1/operations/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(1, database.count("expense_operations"));
        assertEquals("2500", database.queryFirstColumn("SELECT amount FROM expense_operations WHERE id = ?", id));
        assertEquals("7500", balance(ACCOUNT_ID));
    }

    @Test
    void registersTransferAnswers201WithLocationIdPersistedRowAndMovesBalance() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/transfers",
            transfer(ACCOUNT_ID, SECOND_ACCOUNT_ID, 4000, "2026-10-03"));

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("http://localhost:" + port + "/api/v1/operations/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(1, database.count("transfer_operations"));
        assertEquals("4000", database.queryFirstColumn("SELECT amount FROM transfer_operations WHERE id = ?", id));
        assertEquals("6000", balance(ACCOUNT_ID));
        assertEquals("4000", balance(SECOND_ACCOUNT_ID));
    }

    @Test
    void registerExpenseWithoutAmountCentsAnswers400AndPersistsNothing() throws Exception {
        String body = "{\"accountId\":\"" + ACCOUNT_ID + "\",\"categoryId\":\"" + EXPENSE_CATEGORY_ID
            + "\",\"operationDate\":\"2026-10-02\"}";

        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses", body);

        assertProblem(response, 400, "Field 'amountCents' is required");
        assertEquals(0, database.count("expense_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void registerIncomeWithoutAmountCentsAnswers400() throws Exception {
        String body = "{\"accountId\":\"" + ACCOUNT_ID + "\",\"categoryId\":\"" + INCOME_CATEGORY_ID
            + "\",\"operationDate\":\"2026-10-02\"}";

        HttpResponse<String> response = send("POST", "/api/v1/operations/incomes", body);

        assertProblem(response, 400, "Field 'amountCents' is required");
        assertEquals(0, database.count("income_operations"));
    }

    @Test
    void registerTransferWithoutSourceAccountIdAnswers400() throws Exception {
        String body = "{\"targetAccountId\":\"" + SECOND_ACCOUNT_ID
            + "\",\"amountCents\":100,\"operationDate\":\"2026-10-03\"}";

        HttpResponse<String> response = send("POST", "/api/v1/operations/transfers", body);

        assertProblem(response, 400, "Field 'sourceAccountId' is required");
        assertEquals(0, database.count("transfer_operations"));
    }

    @Test
    void registerWithNegativeAmountCentsAnswers400AndPersistsNothing() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/incomes",
            accountOperation(ACCOUNT_ID, INCOME_CATEGORY_ID, -1, "2026-10-01"));

        assertEquals(400, response.statusCode());
        assertEquals(0, database.count("income_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void registerWithZeroAmountCentsAnswers400AndPersistsNothing() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, EXPENSE_CATEGORY_ID, 0, "2026-10-01"));

        assertEquals(400, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
    }

    @Test
    void registerExpenseOfExactlyTheBalanceSucceedsAndLeavesZero() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, EXPENSE_CATEGORY_ID, 10000, "2026-10-02"));

        assertEquals(201, response.statusCode());
        assertEquals("0", balance(ACCOUNT_ID));
    }

    @Test
    void registerExpenseAboveBalanceAnswers409InsufficientBalanceAndKeepsState() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, EXPENSE_CATEGORY_ID, 10001, "2026-10-02"));

        assertEquals(409, response.statusCode());
        assertEquals("INSUFFICIENT_BALANCE", mapper.readTree(response.body()).path("code").asString());
        assertEquals(0, database.count("expense_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void registerTransferAboveBalanceAnswers409InsufficientBalanceAndKeepsState() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/transfers",
            transfer(ACCOUNT_ID, SECOND_ACCOUNT_ID, 10001, "2026-10-03"));

        assertEquals(409, response.statusCode());
        assertEquals("INSUFFICIENT_BALANCE", mapper.readTree(response.body()).path("code").asString());
        assertEquals(0, database.count("transfer_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
        assertEquals("0", balance(SECOND_ACCOUNT_ID));
    }

    @Test
    void registerIncomeInUnknownAccountAnswers404() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/incomes",
            accountOperation(UUID.randomUUID(), INCOME_CATEGORY_ID, 100, "2026-10-01"));

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("income_operations"));
    }

    @Test
    void registerExpenseInAnotherUsersAccountAnswers404AndKeepsTheirBalance() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(OTHER_ACCOUNT_ID, EXPENSE_CATEGORY_ID, 100, "2026-10-01"));

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
        assertEquals("10000", balance(OTHER_ACCOUNT_ID));
    }

    @Test
    void registerExpenseWithAnotherUsersCategoryAnswers404() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, OTHER_CATEGORY_ID, 100, "2026-10-01"));

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("expense_operations"));
    }

    @Test
    void registerTransferToAnotherUsersAccountAnswers404AndKeepsBalances() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/transfers",
            transfer(ACCOUNT_ID, OTHER_ACCOUNT_ID, 100, "2026-10-03"));

        assertEquals(404, response.statusCode());
        assertEquals(0, database.count("transfer_operations"));
        assertEquals("10000", balance(ACCOUNT_ID));
        assertEquals("10000", balance(OTHER_ACCOUNT_ID));
    }

    // ---------- cancel ----------

    @Test
    void cancelsExpenseAnswers204RestoresBalanceAndDetailsShowCancelledAtFromTheClock() throws Exception {
        UUID id = registerExpenseOf(2500);

        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(204, cancel.statusCode());
        assertEquals("", cancel.body());
        assertEquals("10000", balance(ACCOUNT_ID));
        assertEquals(1, database.count("reversals"));
        JsonNode details = mapper.readTree(send("GET", "/api/v1/operations/" + id, null).body());
        assertEquals("CANCELLED", details.path("status").asString());
        assertEquals(NOW, Instant.parse(details.path("cancelledAt").asString()));
    }

    @Test
    void cancelsIncomeAnswers204AndDebitsBalance() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertIncome(id, ACCOUNT_ID, INCOME_CATEGORY_ID, 3000, "2026-10-01", "ACTIVE");

        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(204, cancel.statusCode());
        assertEquals("7000", balance(ACCOUNT_ID));
        assertEquals("CANCELLED",
            database.queryFirstColumn("SELECT status FROM income_operations WHERE id = ?", id.toString()));
    }

    @Test
    void cancelWithBalanceTooLowToRevertAnswers409AndKeepsOperationActive() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertIncome(id, ACCOUNT_ID, INCOME_CATEGORY_ID, 10001, "2026-10-01", "ACTIVE");

        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(409, cancel.statusCode());
        assertEquals("INSUFFICIENT_BALANCE", mapper.readTree(cancel.body()).path("code").asString());
        assertEquals(0, database.count("reversals"));
        assertEquals("ACTIVE",
            database.queryFirstColumn("SELECT status FROM income_operations WHERE id = ?", id.toString()));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void cancelTwiceAnswers409OperationAlreadyCancelledAndRevertsOnlyOnce() throws Exception {
        UUID id = registerExpenseOf(2500);
        send("POST", "/api/v1/operations/" + id + "/cancel", null);

        HttpResponse<String> second = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(409, second.statusCode());
        assertEquals("OPERATION_ALREADY_CANCELLED", mapper.readTree(second.body()).path("code").asString());
        assertEquals(1, database.count("reversals"));
        assertEquals("10000", balance(ACCOUNT_ID));
    }

    @Test
    void cancelTransferAnswers404AndKeepsBalances() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertTransfer(id, ACCOUNT_ID, SECOND_ACCOUNT_ID, 100, "2026-10-01");

        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(404, cancel.statusCode());
        assertEquals(0, database.count("reversals"));
    }

    @Test
    void cancelUnknownOperationAnswers404() throws Exception {
        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + UUID.randomUUID() + "/cancel", null);

        assertEquals(404, cancel.statusCode());
    }

    @Test
    void cancelAnotherUsersOperationAnswers404AndKeepsItActive() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000009");
        database.insertExpense(id, OTHER_ACCOUNT_ID, OTHER_CATEGORY_ID, 500, "2026-10-01", "ACTIVE");

        HttpResponse<String> cancel = send("POST", "/api/v1/operations/" + id + "/cancel", null);

        assertEquals(404, cancel.statusCode());
        assertEquals("ACTIVE",
            database.queryFirstColumn("SELECT status FROM expense_operations WHERE id = ?", id.toString()));
        assertEquals("10000", balance(OTHER_ACCOUNT_ID));
        assertEquals(0, database.count("reversals"));
    }

    @Test
    void cancelWithMalformedUuidAnswers400() throws Exception {
        HttpResponse<String> cancel = send("POST", "/api/v1/operations/not-a-uuid/cancel", null);

        assertEquals(400, cancel.statusCode());
    }

    // ---------- details and JSON shapes ----------

    @Test
    void getsActiveExpenseWithAccountAndCategoryAndNullTransferAndCancelledAt() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertExpense(id, ACCOUNT_ID, EXPENSE_CATEGORY_ID, 2599, "2026-10-02", "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/operations/" + id, null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(9, body.size());
        assertEquals(id.toString(), body.path("id").asString());
        assertEquals("EXPENSE", body.path("type").asString());
        assertEquals(2599L, body.path("amountCents").asLong());
        assertTrue(body.path("amountCents").isIntegralNumber());
        assertEquals("2026-10-02", body.path("operationDate").asString());
        assertEquals("ACTIVE", body.path("status").asString());
        assertTrue(body.path("cancelledAt").isNull());
        assertEquals(ACCOUNT_ID.toString(), body.path("account").path("id").asString());
        assertEquals("Wallet", body.path("account").path("name").asString());
        assertEquals(EXPENSE_CATEGORY_ID.toString(), body.path("category").path("id").asString());
        assertEquals("Food", body.path("category").path("name").asString());
        assertTrue(body.has("transfer"));
        assertTrue(body.path("transfer").isNull());
    }

    @Test
    void getsIncomeWithTypeIncomeAndSameShapeAsExpense() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertIncome(id, ACCOUNT_ID, INCOME_CATEGORY_ID, 100, "2026-10-02", "ACTIVE");

        JsonNode body = mapper.readTree(send("GET", "/api/v1/operations/" + id, null).body());

        assertEquals("INCOME", body.path("type").asString());
        assertEquals("Salary", body.path("category").path("name").asString());
        assertTrue(body.path("transfer").isNull());
        assertTrue(body.path("cancelledAt").isNull());
    }

    @Test
    void getsTransferWithExplicitNullStatusCancelledAtAccountAndCategory() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000001");
        database.insertTransfer(id, ACCOUNT_ID, SECOND_ACCOUNT_ID, 700, "2026-10-03");

        HttpResponse<String> response = send("GET", "/api/v1/operations/" + id, null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(9, body.size());
        assertEquals("TRANSFER", body.path("type").asString());
        assertEquals(700L, body.path("amountCents").asLong());
        for (String field : new String[] {"status", "cancelledAt", "account", "category"}) {
            assertTrue(body.has(field), field + " must be present");
            assertTrue(body.path(field).isNull(), field + " must be null");
        }
        JsonNode transfer = body.path("transfer");
        assertEquals(ACCOUNT_ID.toString(), transfer.path("sourceAccount").path("id").asString());
        assertEquals("Wallet", transfer.path("sourceAccount").path("name").asString());
        assertEquals(SECOND_ACCOUNT_ID.toString(), transfer.path("targetAccount").path("id").asString());
        assertEquals("Savings", transfer.path("targetAccount").path("name").asString());
    }

    @Test
    void getUnknownOperationAnswers404() throws Exception {
        assertEquals(404, send("GET", "/api/v1/operations/" + UUID.randomUUID(), null).statusCode());
    }

    @Test
    void getAnotherUsersOperationAnswers404WithoutLeakingData() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000009");
        database.insertExpense(id, OTHER_ACCOUNT_ID, OTHER_CATEGORY_ID, 500, "2026-10-01", "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/operations/" + id, null);

        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("Theirs"));
    }

    @Test
    void getAnotherUsersTransferAnswers404() throws Exception {
        UUID id = UUID.fromString("20000000-0000-4000-8000-000000000009");
        UUID otherSecond = UUID.fromString("10000000-0000-4000-8000-000000000004");
        database.insertAccount(otherSecond, OTHER_USER_ID, "Theirs 2", 0, "ACTIVE");
        database.insertTransfer(id, OTHER_ACCOUNT_ID, otherSecond, 500, "2026-10-01");

        assertEquals(404, send("GET", "/api/v1/operations/" + id, null).statusCode());
    }

    // ---------- history ----------

    @Test
    void historyWithoutParametersAnswersDefaultsPage1PageSize20() throws Exception {
        JsonNode body = mapper.readTree(send("GET", "/api/v1/operations", null).body());

        assertEquals(3, body.size());
        assertEquals(1, body.path("page").asInt());
        assertEquals(20, body.path("pageSize").asInt());
        assertTrue(body.path("items").isArray());
        assertEquals(0, body.path("items").size());
        assertFalse(body.has("total"));
    }

    @Test
    void historyIsOrderedNewestFirstByDateThenIdDescending() throws Exception {
        seedHistory();

        JsonNode items = history("");

        assertEquals(5, items.size());
        assertEquals(opId(4), items.get(0).path("id").asString());
        assertEquals(opId(3), items.get(1).path("id").asString());
        assertEquals(opId(2), items.get(2).path("id").asString());
        assertEquals(opId(1), items.get(3).path("id").asString());
        assertEquals(opId(5), items.get(4).path("id").asString());
    }

    @Test
    void historyMixesShapesOfTransfersAndIncomeAndExpense() throws Exception {
        seedHistory();

        JsonNode items = history("");

        assertEquals("TRANSFER", items.get(0).path("type").asString());
        assertTrue(items.get(0).path("status").isNull());
        assertTrue(items.get(0).path("account").isNull());
        assertEquals("Wallet", items.get(0).path("transfer").path("sourceAccount").path("name").asString());
        assertEquals("EXPENSE", items.get(1).path("type").asString());
        assertEquals("ACTIVE", items.get(1).path("status").asString());
        assertTrue(items.get(1).path("transfer").isNull());
    }

    @Test
    void historyFiltersByType() throws Exception {
        seedHistory();

        assertEquals(2, history("?type=INCOME").size());
        assertEquals(2, history("?type=EXPENSE").size());
        JsonNode transfers = history("?type=TRANSFER");
        assertEquals(1, transfers.size());
        assertEquals(opId(4), transfers.get(0).path("id").asString());
    }

    @Test
    void historyFiltersByAccountIncludingTransfersWhereItIsSourceOrTarget() throws Exception {
        seedHistory();

        JsonNode ofSavings = history("?accountId=" + SECOND_ACCOUNT_ID);

        assertEquals(1, ofSavings.size());
        assertEquals(opId(4), ofSavings.get(0).path("id").asString());
        assertEquals(5, history("?accountId=" + ACCOUNT_ID).size());
    }

    @Test
    void historyFiltersByCategory() throws Exception {
        seedHistory();

        JsonNode food = history("?categoryId=" + EXPENSE_CATEGORY_ID);

        assertEquals(2, food.size());
        assertEquals(opId(3), food.get(0).path("id").asString());
        assertEquals(opId(2), food.get(1).path("id").asString());
    }

    @Test
    void historyFiltersByDateRangeInclusiveOfBothBounds() throws Exception {
        seedHistory();

        JsonNode items = history("?from=2026-08-01&to=2026-08-02");

        assertEquals(3, items.size());
        assertEquals(opId(3), items.get(0).path("id").asString());
        assertEquals(opId(2), items.get(1).path("id").asString());
        assertEquals(opId(1), items.get(2).path("id").asString());
    }

    @Test
    void historyWithFromEqualToReturnsThatDayOnly() throws Exception {
        seedHistory();

        JsonNode items = history("?from=2026-07-01&to=2026-07-01");

        assertEquals(1, items.size());
        assertEquals(opId(5), items.get(0).path("id").asString());
    }

    @Test
    void historyPagesAndEchoesTheAppliedPageAndSize() throws Exception {
        seedHistory();

        JsonNode second = mapper.readTree(send("GET", "/api/v1/operations?page=2&pageSize=2", null).body());

        assertEquals(2, second.path("page").asInt());
        assertEquals(2, second.path("pageSize").asInt());
        assertEquals(2, second.path("items").size());
        assertEquals(opId(2), second.path("items").get(0).path("id").asString());
        assertEquals(opId(1), second.path("items").get(1).path("id").asString());
    }

    @Test
    void historyLastPageHasFewerItemsThanPageSize() throws Exception {
        seedHistory();

        JsonNode third = mapper.readTree(send("GET", "/api/v1/operations?page=3&pageSize=2", null).body());

        assertEquals(1, third.path("items").size());
        assertEquals(opId(5), third.path("items").get(0).path("id").asString());
    }

    @Test
    void historyPastTheEndAnswersEmptyItems() throws Exception {
        seedHistory();

        HttpResponse<String> response = send("GET", "/api/v1/operations?page=4&pageSize=2", null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(0, body.path("items").size());
        assertEquals(4, body.path("page").asInt());
    }

    @Test
    void historyAcceptsPageSize100() throws Exception {
        JsonNode body = mapper.readTree(send("GET", "/api/v1/operations?pageSize=100", null).body());

        assertEquals(100, body.path("pageSize").asInt());
    }

    @Test
    void historyShowsCancelledOperationWithCancelledAt() throws Exception {
        UUID id = registerExpenseOf(100);
        send("POST", "/api/v1/operations/" + id + "/cancel", null);

        JsonNode items = history("");

        assertEquals(1, items.size());
        assertEquals("CANCELLED", items.get(0).path("status").asString());
        assertEquals(NOW, Instant.parse(items.get(0).path("cancelledAt").asString()));
    }

    @Test
    void historyNeverIncludesAnotherUsersOperations() throws Exception {
        database.insertExpense(UUID.fromString("20000000-0000-4000-8000-000000000009"),
            OTHER_ACCOUNT_ID, OTHER_CATEGORY_ID, 500, "2026-10-01", "ACTIVE");
        UUID otherSecond = UUID.fromString("10000000-0000-4000-8000-000000000004");
        database.insertAccount(otherSecond, OTHER_USER_ID, "Theirs 2", 0, "ACTIVE");
        database.insertTransfer(UUID.fromString("20000000-0000-4000-8000-000000000008"),
            OTHER_ACCOUNT_ID, otherSecond, 500, "2026-10-01");
        database.insertIncome(UUID.fromString("20000000-0000-4000-8000-000000000001"),
            ACCOUNT_ID, INCOME_CATEGORY_ID, 100, "2026-10-01", "ACTIVE");

        JsonNode items = history("");

        assertEquals(1, items.size());
        assertEquals(opId(1), items.get(0).path("id").asString());
    }

    @Test
    void historyFilteredByAnotherUsersAccountAnswersEmpty() throws Exception {
        database.insertExpense(UUID.fromString("20000000-0000-4000-8000-000000000009"),
            OTHER_ACCOUNT_ID, OTHER_CATEGORY_ID, 500, "2026-10-01", "ACTIVE");

        assertEquals(0, history("?accountId=" + OTHER_ACCOUNT_ID).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "?page=0", "?page=-1", "?pageSize=0", "?pageSize=101", "?page=abc", "?pageSize=1.5",
        "?from=2026-10-02&to=2026-10-01", "?type=income", "?type=LOAN", "?accountId=not-a-uuid",
        "?categoryId=123", "?from=2026-13-01", "?to=yesterday", "?from=01/10/2026"
    })
    void historyRejectsInvalidParametersWith400(String query) throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/operations" + query, null);

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("")
            .startsWith("application/problem+json"));
    }

    // ---------- helpers ----------

    private void seedHistory() {
        database.insertIncome(uuid(1), ACCOUNT_ID, INCOME_CATEGORY_ID, 1000, "2026-08-01", "ACTIVE");
        database.insertExpense(uuid(2), ACCOUNT_ID, EXPENSE_CATEGORY_ID, 200, "2026-08-02", "ACTIVE");
        database.insertExpense(uuid(3), ACCOUNT_ID, EXPENSE_CATEGORY_ID, 300, "2026-08-02", "ACTIVE");
        database.insertTransfer(uuid(4), ACCOUNT_ID, SECOND_ACCOUNT_ID, 400, "2026-08-03");
        database.insertIncome(uuid(5), ACCOUNT_ID, INCOME_CATEGORY_ID, 500, "2026-07-01", "ACTIVE");
    }

    private static UUID uuid(int n) {
        return UUID.fromString(opId(n));
    }

    private static String opId(int n) {
        return "20000000-0000-4000-8000-" + String.format("%012d", n);
    }

    private JsonNode history(String query) throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/operations" + query, null);
        assertEquals(200, response.statusCode(), response.body());
        return mapper.readTree(response.body()).path("items");
    }

    private UUID registerExpenseOf(long cents) throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/operations/expenses",
            accountOperation(ACCOUNT_ID, EXPENSE_CATEGORY_ID, cents, "2026-10-02"));
        assertEquals(201, response.statusCode());
        return UUID.fromString(mapper.readTree(response.body()).path("id").asString());
    }

    private String balance(UUID accountId) {
        return database.queryFirstColumn("SELECT balance FROM accounts WHERE id = ?", accountId.toString());
    }

    private static String accountOperation(UUID accountId, UUID categoryId, long cents, String date) {
        return "{\"accountId\":\"" + accountId + "\",\"categoryId\":\"" + categoryId
            + "\",\"amountCents\":" + cents + ",\"operationDate\":\"" + date + "\"}";
    }

    private static String transfer(UUID sourceId, UUID targetId, long cents, String date) {
        return "{\"sourceAccountId\":\"" + sourceId + "\",\"targetAccountId\":\"" + targetId
            + "\",\"amountCents\":" + cents + ",\"operationDate\":\"" + date + "\"}";
    }

    private void assertProblem(HttpResponse<String> response, int status, String detail) throws Exception {
        assertEquals(status, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals(detail, mapper.readTree(response.body()).path("detail").asString());
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
}
