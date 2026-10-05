package com.rauldev.personalfinance.entry.web.error;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
import com.rauldev.personalfinance.entry.security.SingleUserNotProvisionedException;
import com.rauldev.personalfinance.infrastructure.persistence.CorruptedPersistedDataException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(ApiExceptionHandlerTest.FailingController.class)
class ApiExceptionHandlerTest {

    private static final String SENSITIVE = "secret-table-name-and-password";

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteUrl(DynamicPropertyRegistry registry) {
        registry.add("personal-finance.sqlite.url",
            () -> "jdbc:sqlite:" + tempDir.resolve("api.db").toAbsolutePath());
        registry.add("personal-finance.single-user-id", () -> "00000000-0000-4000-8000-000000000001");
    }

    @LocalServerPort
    private int port;

    @Test
    void mapsResourceNotFoundExceptionToProblemDetail404() throws Exception {
        HttpResponse<String> response = get("/test-errors/not-found");

        assertEquals(404, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Resource not found\""));
        assertTrue(response.body().contains("\"detail\":\"Account does not exist\""));
    }

    @Test
    void mapsIllegalArgumentExceptionToProblemDetail400() throws Exception {
        HttpResponse<String> response = get("/test-errors/bad-request");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Invalid request\""));
        assertTrue(response.body().contains("\"detail\":\"Amount must be positive\""));
    }

    @Test
    void mapsBusinessRuleViolationExceptionToProblemDetail409WithCode() throws Exception {
        HttpResponse<String> response = get("/test-errors/business-rule");

        assertEquals(409, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Business rule violation\""));
        assertTrue(response.body().contains("\"detail\":\"Account balance is insufficient\""));
        assertTrue(response.body().contains("\"code\":\"INSUFFICIENT_BALANCE\""));
    }

    @Test
    void infrastructureIllegalStateExceptionStillAnswers500GenericProblemDetail() throws Exception {
        HttpResponse<String> response = get("/test-errors/nested-transaction");

        assertEquals(500, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Internal server error\""));
        assertTrue(response.body().contains("\"detail\":\"Unexpected error\""));
        assertFalse(response.body().contains("Nested transactions"));
        assertFalse(response.body().contains("\"code\""));
    }

    @Test
    void unmappedExceptionAnswers500ProblemDetailWithoutLeakingMessage() throws Exception {
        HttpResponse<String> response = get("/test-errors/unexpected");

        assertEquals(500, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Internal server error\""));
        assertTrue(response.body().contains("\"detail\":\"Unexpected error\""));
        assertFalse(response.body().contains(SENSITIVE));
    }

    @Test
    void corruptedPersistedDataAnswers500WithoutLeakingCauseOrRowDetails() throws Exception {
        HttpResponse<String> response = get("/test-errors/corrupted");

        assertEquals(500, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"detail\":\"Unexpected error\""));
        assertFalse(response.body().contains(SENSITIVE));
        assertFalse(response.body().contains("Corrupted persisted data"));
        assertFalse(response.body().contains("accounts"));
    }

    @Test
    void unknownRouteKeepsFrameworkProblemDetail404() throws Exception {
        HttpResponse<String> response = get("/test-errors/does-not-exist");

        assertEquals(404, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void malformedJsonKeepsFrameworkProblemDetail400() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test-errors/echo"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{not json"))
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void invalidUuidPathVariableAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = get("/test-errors/by-id/not-a-uuid");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void validUuidPathVariableIsAccepted() throws Exception {
        HttpResponse<String> response = get("/test-errors/by-id/00000000-0000-4000-8000-000000000002");

        assertEquals(200, response.statusCode());
        assertEquals("00000000-0000-4000-8000-000000000002", response.body());
    }

    @Test
    void invalidDatePathVariableAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = get("/test-errors/by-date/04-10-2026");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void isoDatePathVariableIsAccepted() throws Exception {
        HttpResponse<String> response = get("/test-errors/by-date/2026-10-04");

        assertEquals(200, response.statusCode());
        assertEquals("2026-10-04", response.body());
    }

    @Test
    void localizedShortDateInQueryParameterAnswers400ProblemDetail() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test-errors/by-date?date=10/4/26"))
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US")
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void isoDateInQueryParameterIsAcceptedWithEnglishLocale() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test-errors/by-date?date=2026-10-04"))
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US")
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("2026-10-04", response.body());
    }

    @Test
    void unknownJsonPropertyAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json",
            "{\"amountCents\":1000,\"extra\":true}");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void integerJsonNumberIsAcceptedForLongField() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json", "{\"amountCents\":1000}");

        assertEquals(200, response.statusCode());
        assertEquals("1000", response.body());
    }

    @Test
    void fractionalJsonNumberForLongFieldAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json", "{\"amountCents\":10.5}");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void wholeFloatJsonNumberForLongFieldAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json", "{\"amountCents\":10.0}");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void invalidEnumValueAnswers400ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json",
            "{\"amountCents\":1000,\"kind\":\"NOT_A_KIND\"}");

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void validEnumValueIsAccepted() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "application/json",
            "{\"amountCents\":1000,\"kind\":\"WEEKLY\"}");

        assertEquals(200, response.statusCode());
    }

    @Test
    void missingRequestBodyAnswers400ProblemDetail() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test-errors/typed"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void unsupportedMethodAnswers405ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/not-found", "application/json", "{}");

        assertEquals(405, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void unsupportedMediaTypeAnswers415ProblemDetail() throws Exception {
        HttpResponse<String> response = post("/test-errors/typed", "text/plain", "amountCents=1000");

        assertEquals(415, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
    }

    @Test
    void singleUserNotProvisionedAnswers500WithoutLeakingMessage() throws Exception {
        HttpResponse<String> response = get("/test-errors/single-user-not-provisioned");

        assertEquals(500, response.statusCode());
        assertTrue(contentType(response).startsWith("application/problem+json"));
        assertTrue(response.body().contains("\"title\":\"Internal server error\""));
        assertTrue(response.body().contains("\"detail\":\"Unexpected error\""));
        assertFalse(response.body().contains("00000000-0000-4000-8000-000000000009"));
        assertFalse(response.body().contains("users table"));
    }

    private HttpResponse<String> post(String path, String mediaType, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", mediaType)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", "application/json")
            .GET()
            .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String contentType(HttpResponse<String> response) {
        return response.headers().firstValue("Content-Type").orElse("");
    }

    enum Kind { WEEKLY, MONTHLY }

    record TypedBody(Long amountCents, Kind kind) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/test-errors/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Account does not exist");
        }

        @GetMapping("/test-errors/bad-request")
        String badRequest() {
            throw new IllegalArgumentException("Amount must be positive");
        }

        @GetMapping("/test-errors/corrupted")
        String corrupted() {
            throw new CorruptedPersistedDataException("accounts", "row-1", new IllegalArgumentException(SENSITIVE));
        }

        @GetMapping("/test-errors/business-rule")
        String businessRule() {
            throw new BusinessRuleViolationException(BusinessRuleCode.INSUFFICIENT_BALANCE,
                "Account balance is insufficient");
        }

        @GetMapping("/test-errors/nested-transaction")
        String nestedTransaction() {
            throw new IllegalStateException("Nested transactions are not supported");
        }

        @PostMapping("/test-errors/echo")
        String echo(@RequestBody java.util.Map<String, Object> body) {
            return "ok";
        }

        @GetMapping("/test-errors/by-id/{id}")
        String byId(@PathVariable UUID id) {
            return id.toString();
        }

        @GetMapping("/test-errors/by-date/{date}")
        String byDate(@PathVariable LocalDate date) {
            return date.toString();
        }

        @GetMapping("/test-errors/by-date")
        String byDateParam(@RequestParam LocalDate date) {
            return date.toString();
        }

        @PostMapping("/test-errors/typed")
        String typed(@RequestBody TypedBody body) {
            return String.valueOf(body.amountCents());
        }

        @GetMapping("/test-errors/single-user-not-provisioned")
        String singleUserNotProvisioned() {
            throw new SingleUserNotProvisionedException(UUID.fromString("00000000-0000-4000-8000-000000000009"));
        }

        @GetMapping("/test-errors/unexpected")
        String unexpected() {
            throw new IllegalStateException(SENSITIVE);
        }
    }
}
