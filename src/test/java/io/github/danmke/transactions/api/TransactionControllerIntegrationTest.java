package io.github.danmke.transactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-06T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.14-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsPurchaseTransactionWithNegativeAmountAndDeterministicEventDate() throws Exception {
        long accountId = createAccount("11122233344");
        String json = transactionJson(accountId, 1, "123.45");

        ResponseEntity<String> response = postJson("/transactions", json);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("transaction_id").asLong()).isPositive();
        assertThat(body.get("account_id").asLong()).isEqualTo(accountId);
        assertThat(body.get("operation_type_id").asInt()).isEqualTo(1);
        // NORMAL_PURCHASE normalizes to a negative amount.
        assertThat(new BigDecimal(body.get("amount").asText())).isEqualByComparingTo("-123.45");
        assertThat(OffsetDateTime.parse(body.get("event_date").asText()).toInstant()).isEqualTo(FIXED_INSTANT);

        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath())
                .isEqualTo("/transactions/" + body.get("transaction_id").asLong());
    }

    @Test
    void creditVoucherKeepsPositiveAmount() throws Exception {
        long accountId = createAccount("55566677788");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 4, "60.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(new BigDecimal(body.get("amount").asText())).isEqualByComparingTo("60.00");
    }

    @Test
    void rejectsMissingAccountIdWith400() throws Exception {
        ResponseEntity<String> response = postJson("/transactions",
                "{\"operation_type_id\": 1, \"amount\": 10.00}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsMissingOperationTypeIdWith400() throws Exception {
        ResponseEntity<String> response = postJson("/transactions",
                "{\"account_id\": 1, \"amount\": 10.00}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsMissingAmountWith400() throws Exception {
        ResponseEntity<String> response = postJson("/transactions",
                "{\"account_id\": 1, \"operation_type_id\": 1}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAmountWithTooManyFractionDigitsWith400() throws Exception {
        ResponseEntity<String> response = postJson("/transactions",
                "{\"account_id\": 1, \"operation_type_id\": 1, \"amount\": 10.123}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsAmountWithTooManyIntegerDigitsWith400() throws Exception {
        ResponseEntity<String> response = postJson("/transactions",
                "{\"account_id\": 1, \"operation_type_id\": 1, \"amount\": 123456789012345678.00}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsNegativeAmountWith400() throws Exception {
        // Negative sign is checked before the account lookup, so a missing account is irrelevant here.
        ResponseEntity<String> response = postJson("/transactions",
                "{\"account_id\": 999999, \"operation_type_id\": 1, \"amount\": -10.00}");

        assertProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void returns404WhenAccountDoesNotExist() throws Exception {
        ResponseEntity<String> response = postJson("/transactions", transactionJson(99999999L, 1, "10.00"));

        assertProblemDetail(response, HttpStatus.NOT_FOUND);
    }

    @Test
    void returns422WhenOperationTypeIsUnknown() throws Exception {
        long accountId = createAccount("22233344455");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 99, "10.00"));

        assertProblemDetail(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void returns422WhenAmountIsZero() throws Exception {
        long accountId = createAccount("33344455566");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 1, "0.00"));

        assertProblemDetail(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private long createAccount(String documentNumber) throws Exception {
        ResponseEntity<String> response = postJson("/accounts",
                "{\"document_number\": \"" + documentNumber + "\"}");
        return objectMapper.readTree(response.getBody()).get("account_id").asLong();
    }

    private static String transactionJson(long accountId, int operationTypeId, String amount) {
        return "{\"account_id\": " + accountId
                + ", \"operation_type_id\": " + operationTypeId
                + ", \"amount\": " + amount + "}";
    }

    private void assertValidationProblemDetail(ResponseEntity<String> response, HttpStatus expectedStatus)
            throws Exception {
        JsonNode body = assertProblemDetail(response, expectedStatus);
        assertThat(body.get("errors").isArray()).isTrue();
        assertThat(body.get("errors")).isNotEmpty();
    }

    private JsonNode assertProblemDetail(ResponseEntity<String> response, HttpStatus expectedStatus) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType())
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asInt()).isEqualTo(expectedStatus.value());
        assertThat(body.get("detail").asText()).isNotBlank();
        return body;
    }

    private ResponseEntity<String> postJson(String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(path, new HttpEntity<>(json, headers), String.class);
    }
}
