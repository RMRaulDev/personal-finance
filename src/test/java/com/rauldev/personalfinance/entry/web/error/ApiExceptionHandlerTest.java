package com.rauldev.personalfinance.entry.web.error;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.rauldev.personalfinance.application.exception.ResourceNotFoundException;
import com.rauldev.personalfinance.domain.BusinessRuleCode;
import com.rauldev.personalfinance.domain.BusinessRuleViolationException;
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

        @GetMapping("/test-errors/unexpected")
        String unexpected() {
            throw new IllegalStateException(SENSITIVE);
        }
    }
}
