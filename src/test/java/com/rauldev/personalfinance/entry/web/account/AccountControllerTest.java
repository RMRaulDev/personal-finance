package com.rauldev.personalfinance.entry.web.account;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.rauldev.personalfinance.entry.SqliteTestDatabase;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID OWN_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_ACCOUNT_ID = UUID.fromString("10000000-0000-4000-8000-000000000002");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @TempDir
    static Path tempDir;

    static SqliteTestDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        database = new SqliteTestDatabase(tempDir.resolve("accounts.db"));
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
    }

    @Test
    void createsAccountAnswers201WithLocationAndId() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/accounts", "{\"name\":\"Wallet\"}");

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("http://localhost:" + port + "/api/v1/accounts/" + id,
            response.headers().firstValue("Location").orElseThrow());
        assertEquals(USER_ID.toString(),
            database.queryFirstColumn("SELECT user_id FROM accounts WHERE id = ?", id));
        assertEquals("Wallet", database.queryFirstColumn("SELECT name FROM accounts WHERE id = ?", id));
        assertEquals(1, database.count("accounts"));
    }

    @Test
    void createAccountWithoutNameAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/accounts", "{}");

        assertProblem(response, 400, "Field 'name' is required");
        assertEquals(0, database.count("accounts"));
    }

    @Test
    void createAccountWithBlankNameAnswers400WithCoreMessage() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/accounts", "{\"name\":\"  \"}");

        assertProblem(response, 400, "Account name cannot be empty");
        assertEquals(0, database.count("accounts"));
    }

    @Test
    void createAccountWithDuplicateNameAnswers409WithCode() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 0, "ACTIVE");

        HttpResponse<String> response = send("POST", "/api/v1/accounts", "{\"name\":\"Wallet\"}");

        assertEquals(409, response.statusCode());
        assertEquals("ACCOUNT_NAME_ALREADY_EXISTS", mapper.readTree(response.body()).path("code").asString());
        assertEquals(1, database.count("accounts"));
    }

    @Test
    void createAccountAllowsSameNameAsAnotherUsersAccount() throws Exception {
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Wallet", 0, "ACTIVE");

        HttpResponse<String> response = send("POST", "/api/v1/accounts", "{\"name\":\"Wallet\"}");

        assertEquals(201, response.statusCode());
    }

    @Test
    void modifiesAccountAnswers204AndPersistsNewName() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 0, "ACTIVE");

        HttpResponse<String> response = send("PATCH", "/api/v1/accounts/" + OWN_ACCOUNT_ID, "{\"name\":\"Savings\"}");

        assertEquals(204, response.statusCode());
        assertEquals("", response.body());
        assertEquals("Savings",
            database.queryFirstColumn("SELECT name FROM accounts WHERE id = ?", OWN_ACCOUNT_ID.toString()));
    }

    @Test
    void modifyAccountWithoutNameAnswers400AndKeepsName() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 0, "ACTIVE");

        HttpResponse<String> response = send("PATCH", "/api/v1/accounts/" + OWN_ACCOUNT_ID, "{}");

        assertProblem(response, 400, "Field 'name' is required");
        assertEquals("Wallet",
            database.queryFirstColumn("SELECT name FROM accounts WHERE id = ?", OWN_ACCOUNT_ID.toString()));
    }

    @Test
    void modifyAccountOfAnotherUserAnswers404AndKeepsName() throws Exception {
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Wallet", 0, "ACTIVE");

        HttpResponse<String> response = send("PATCH", "/api/v1/accounts/" + OTHER_ACCOUNT_ID, "{\"name\":\"Mine\"}");

        assertEquals(404, response.statusCode());
        assertEquals("Wallet",
            database.queryFirstColumn("SELECT name FROM accounts WHERE id = ?", OTHER_ACCOUNT_ID.toString()));
    }

    @Test
    void modifyAccountWithDuplicateNameAnswers409WithCode() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 0, "ACTIVE");
        database.insertAccount(UUID.fromString("10000000-0000-4000-8000-000000000003"), USER_ID, "Savings", 0, "ACTIVE");

        HttpResponse<String> response = send("PATCH", "/api/v1/accounts/" + OWN_ACCOUNT_ID, "{\"name\":\"Savings\"}");

        assertEquals(409, response.statusCode());
        assertEquals("ACCOUNT_NAME_ALREADY_EXISTS", mapper.readTree(response.body()).path("code").asString());
        assertEquals("Wallet",
            database.queryFirstColumn("SELECT name FROM accounts WHERE id = ?", OWN_ACCOUNT_ID.toString()));
    }

    @Test
    void getsAccountWithExactlyIdNameBalanceCentsAndStatus() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 123456, "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts/" + OWN_ACCOUNT_ID, null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(4, body.size());
        assertEquals(OWN_ACCOUNT_ID.toString(), body.path("id").asString());
        assertEquals("Wallet", body.path("name").asString());
        assertEquals(123456L, body.path("balanceCents").asLong());
        assertTrue(body.path("balanceCents").isIntegralNumber());
        assertEquals("ACTIVE", body.path("status").asString());
        assertFalse(body.has("userId"));
    }

    @Test
    void getsInactiveAccountWithStatusName() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Wallet", 0, "INACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts/" + OWN_ACCOUNT_ID, null);

        assertEquals(200, response.statusCode());
        assertEquals("INACTIVE", mapper.readTree(response.body()).path("status").asString());
        assertEquals(0L, mapper.readTree(response.body()).path("balanceCents").asLong());
    }

    @Test
    void getAccountOfAnotherUserAnswers404() throws Exception {
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Wallet", 500, "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts/" + OTHER_ACCOUNT_ID, null);

        assertEquals(404, response.statusCode());
        assertFalse(response.body().contains("Wallet"));
    }

    @Test
    void getUnknownAccountAnswers404() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/accounts/" + OWN_ACCOUNT_ID, null);

        assertEquals(404, response.statusCode());
    }

    @Test
    void getAccountWithMalformedUuidAnswers400() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/accounts/not-a-uuid", null);

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
    }

    @Test
    void listsAccountsAsBareArrayOrderedByNameWithExactlyTheFields() throws Exception {
        UUID walletId = UUID.fromString("10000000-0000-4000-8000-000000000011");
        database.insertAccount(walletId, USER_ID, "Wallet", 123456, "ACTIVE");
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Checking", 0, "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts", null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertTrue(body.isArray());
        assertEquals(2, body.size());
        assertEquals("Checking", body.get(0).path("name").asString());
        assertEquals("Wallet", body.get(1).path("name").asString());
        JsonNode wallet = body.get(1);
        assertEquals(4, wallet.size());
        assertEquals(walletId.toString(), wallet.path("id").asString());
        assertEquals(123456L, wallet.path("balanceCents").asLong());
        assertTrue(wallet.path("balanceCents").isIntegralNumber());
        assertEquals("ACTIVE", wallet.path("status").asString());
        assertFalse(wallet.has("userId"));
    }

    @Test
    void listIncludesInactiveAccounts() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Old", 0, "ACTIVE");
        database.execute("UPDATE accounts SET status = 'INACTIVE' WHERE id = ?", OWN_ACCOUNT_ID.toString());

        HttpResponse<String> response = send("GET", "/api/v1/accounts", null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(1, body.size());
        assertEquals("INACTIVE", body.get(0).path("status").asString());
    }

    @Test
    void listExcludesAnotherUsersAccounts() throws Exception {
        database.insertAccount(OWN_ACCOUNT_ID, USER_ID, "Mine", 0, "ACTIVE");
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Theirs", 500, "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts", null);

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(1, body.size());
        assertEquals("Mine", body.get(0).path("name").asString());
        assertFalse(response.body().contains("Theirs"));
    }

    @Test
    void listWithoutAccountsAnswersEmptyArray() throws Exception {
        database.insertAccount(OTHER_ACCOUNT_ID, OTHER_USER_ID, "Theirs", 500, "ACTIVE");

        HttpResponse<String> response = send("GET", "/api/v1/accounts", null);

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
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
