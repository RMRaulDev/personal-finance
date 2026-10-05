package com.rauldev.personalfinance.entry.web.category;

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
class CategoryControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID OWN_CATEGORY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("20000000-0000-4000-8000-000000000002");

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @TempDir
    static Path tempDir;

    static SqliteTestDatabase database;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        database = new SqliteTestDatabase(tempDir.resolve("categories.db"));
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
    void createsExpenseCategoryAnswers201WithIdAndNoLocation() throws Exception {
        HttpResponse<String> response = post("{\"name\":\"Food\",\"type\":\"EXPENSE\"}");

        assertEquals(201, response.statusCode());
        assertTrue(response.headers().firstValue("Location").isEmpty());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(1, body.size());
        String id = body.path("id").asString();
        assertEquals("EXPENSE", database.queryFirstColumn("SELECT type FROM categories WHERE id = ?", id));
        assertEquals("Food", database.queryFirstColumn("SELECT name FROM categories WHERE id = ?", id));
        assertEquals(USER_ID.toString(),
            database.queryFirstColumn("SELECT user_id FROM categories WHERE id = ?", id));
    }

    @Test
    void createsIncomeCategoryWithStoredType() throws Exception {
        HttpResponse<String> response = post("{\"name\":\"Salary\",\"type\":\"INCOME\"}");

        assertEquals(201, response.statusCode());
        String id = mapper.readTree(response.body()).path("id").asString();
        assertEquals("INCOME", database.queryFirstColumn("SELECT type FROM categories WHERE id = ?", id));
    }

    @Test
    void createCategoryWithoutNameAnswers400NamingTheField() throws Exception {
        HttpResponse<String> response = post("{\"type\":\"EXPENSE\"}");

        assertProblem(response, 400, "Field 'name' is required");
        assertEquals(0, database.count("categories"));
    }

    @Test
    void createCategoryWithoutTypeAnswers400NamingTheField() throws Exception {
        HttpResponse<String> response = post("{\"name\":\"Food\"}");

        assertProblem(response, 400, "Field 'type' is required");
        assertEquals(0, database.count("categories"));
    }

    @Test
    void createCategoryWithInvalidTypeAnswers400() throws Exception {
        HttpResponse<String> response = post("{\"name\":\"Food\",\"type\":\"TRANSFER\"}");

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals(0, database.count("categories"));
    }

    @Test
    void createCategoryWithDuplicateNameAnswers409WithCode() throws Exception {
        database.insertCategory(OWN_CATEGORY_ID, USER_ID, "Food", "EXPENSE", "ACTIVE");

        HttpResponse<String> response = post("{\"name\":\"Food\",\"type\":\"INCOME\"}");

        assertEquals(409, response.statusCode());
        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", mapper.readTree(response.body()).path("code").asString());
        assertEquals(1, database.count("categories"));
    }

    @Test
    void createCategoryAllowsSameNameAsAnotherUsersCategory() throws Exception {
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Food", "EXPENSE", "ACTIVE");

        HttpResponse<String> response = post("{\"name\":\"Food\",\"type\":\"EXPENSE\"}");

        assertEquals(201, response.statusCode());
        assertFalse(response.body().isBlank());
    }

    @Test
    void listsCategoriesAsBareArrayOrderedByNameWithExactlyTheFields() throws Exception {
        UUID salaryId = UUID.fromString("20000000-0000-4000-8000-000000000011");
        database.insertCategory(salaryId, USER_ID, "Salary", "INCOME", "ACTIVE");
        database.insertCategory(OWN_CATEGORY_ID, USER_ID, "Food", "EXPENSE", "ACTIVE");

        HttpResponse<String> response = get();

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertTrue(body.isArray());
        assertEquals(2, body.size());
        assertEquals("Food", body.get(0).path("name").asString());
        assertEquals("EXPENSE", body.get(0).path("type").asString());
        JsonNode salary = body.get(1);
        assertEquals(4, salary.size());
        assertEquals(salaryId.toString(), salary.path("id").asString());
        assertEquals("Salary", salary.path("name").asString());
        assertEquals("INCOME", salary.path("type").asString());
        assertEquals("ACTIVE", salary.path("status").asString());
    }

    @Test
    void listIncludesInactiveCategories() throws Exception {
        database.insertCategory(OWN_CATEGORY_ID, USER_ID, "Old", "EXPENSE", "ACTIVE");
        database.execute("UPDATE categories SET status = 'INACTIVE' WHERE id = ?", OWN_CATEGORY_ID.toString());

        HttpResponse<String> response = get();

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(1, body.size());
        assertEquals("INACTIVE", body.get(0).path("status").asString());
    }

    @Test
    void listExcludesAnotherUsersCategories() throws Exception {
        database.insertCategory(OWN_CATEGORY_ID, USER_ID, "Mine", "EXPENSE", "ACTIVE");
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Theirs", "INCOME", "ACTIVE");

        HttpResponse<String> response = get();

        assertEquals(200, response.statusCode());
        JsonNode body = mapper.readTree(response.body());
        assertEquals(1, body.size());
        assertEquals("Mine", body.get(0).path("name").asString());
        assertFalse(response.body().contains("Theirs"));
    }

    @Test
    void listWithoutCategoriesAnswersEmptyArray() throws Exception {
        database.insertCategory(OTHER_CATEGORY_ID, OTHER_USER_ID, "Theirs", "INCOME", "ACTIVE");

        HttpResponse<String> response = get();

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    private HttpResponse<String> get() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/categories"))
            .header("Accept", "application/json")
            .GET()
            .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertProblem(HttpResponse<String> response, int status, String detail) throws Exception {
        assertEquals(status, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals(detail, mapper.readTree(response.body()).path("detail").asString());
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/categories"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
