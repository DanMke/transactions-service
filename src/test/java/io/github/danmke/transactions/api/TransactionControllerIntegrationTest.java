package io.github.danmke.transactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danmke.transactions.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-06T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsPurchaseTransactionAndReadsItBackThroughLocation() throws Exception {
        long accountId = createAccount("11122233344");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 1, "123.45"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("transaction_id").asLong()).isPositive();
        assertThat(body.get("account_id").asLong()).isEqualTo(accountId);
        assertThat(body.get("operation_type_id").asInt()).isEqualTo(1);
        assertThat(new BigDecimal(body.get("amount").asText())).isEqualByComparingTo("-123.45");
        assertThat(OffsetDateTime.parse(body.get("event_date").asText()).toInstant()).isEqualTo(FIXED_INSTANT);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath())
                .isEqualTo("/transactions/" + body.get("transaction_id").asLong());

        // The Location URI must be dereferenceable: fetch the transaction back.
        ResponseEntity<String> fetched = restTemplate.getForEntity(
                response.getHeaders().getLocation().getPath(), String.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetchedBody = objectMapper.readTree(fetched.getBody());
        assertThat(fetchedBody.get("transaction_id").asLong()).isEqualTo(body.get("transaction_id").asLong());
        assertThat(new BigDecimal(fetchedBody.get("amount").asText())).isEqualByComparingTo("-123.45");
    }

    @Test
    void creditVoucherIsStoredWithPositiveAmount() throws Exception {
        long accountId = createAccount("55566677788");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 4, "60.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(new BigDecimal(body.get("amount").asText())).isEqualByComparingTo("60.00");
    }

    @Test
    void listsTransactionsFilteredByAccountNewestFirst() throws Exception {
        long accountId = createAccount("99988877766");

        ResponseEntity<String> firstTransaction = postJson("/transactions", transactionJson(accountId, 1, "123.45"));
        ResponseEntity<String> secondTransaction = postJson("/transactions", transactionJson(accountId, 4, "60.00"));

        long firstTransactionId = objectMapper.readTree(firstTransaction.getBody()).get("transaction_id").asLong();
        long secondTransactionId = objectMapper.readTree(secondTransaction.getBody()).get("transaction_id").asLong();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/transactions?account_id=" + accountId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body).hasSize(2);
        assertThat(body.get(0).get("transaction_id").asLong()).isEqualTo(secondTransactionId);
        assertThat(new BigDecimal(body.get(0).get("amount").asText())).isEqualByComparingTo("60.00");
        assertThat(body.get(1).get("transaction_id").asLong()).isEqualTo(firstTransactionId);
        assertThat(new BigDecimal(body.get(1).get("amount").asText())).isEqualByComparingTo("-123.45");
    }

    @Test
    void returnsProblemDetail422OverTheWireForZeroAmount() throws Exception {
        long accountId = createAccount("33344455566");

        ResponseEntity<String> response = postJson("/transactions", transactionJson(accountId, 1, "0.00"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType())
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("type").asText()).isEqualTo("urn:problem-type:invalid-transaction-amount");
        assertThat(body.get("status").asInt()).isEqualTo(422);
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

    private ResponseEntity<String> postJson(String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(path, new HttpEntity<>(json, headers), String.class);
    }
}
