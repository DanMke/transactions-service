package io.github.danmke.transactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.14-alpine");

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsAccountAndReturns201WithLocation() throws Exception {
        ResponseEntity<String> response = postJson("{\"document_number\": \"12345678900\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode body = objectMapper.readTree(response.getBody());
        long accountId = body.get("account_id").asLong();
        assertThat(accountId).isPositive();
        assertThat(body.get("document_number").asText()).isEqualTo("12345678900");

        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo("/accounts/" + accountId);
    }

    @Test
    void rejectsMissingDocumentNumber() {
        ResponseEntity<String> response = postJson("{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsBlankDocumentNumber() {
        ResponseEntity<String> response = postJson("{\"document_number\": \"   \"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsDocumentNumberExceedingMaxSize() {
        String tooLong = "1".repeat(51);
        ResponseEntity<String> response = postJson("{\"document_number\": \"" + tooLong + "\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void allowsSameDocumentNumberForDifferentAccounts() throws Exception {
        String json = "{\"document_number\": \"99887766554\"}";

        ResponseEntity<String> firstResponse = postJson(json);
        ResponseEntity<String> secondResponse = postJson(json);

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode firstBody = objectMapper.readTree(firstResponse.getBody());
        JsonNode secondBody = objectMapper.readTree(secondResponse.getBody());

        assertThat(firstBody.get("document_number").asText()).isEqualTo("99887766554");
        assertThat(secondBody.get("document_number").asText()).isEqualTo("99887766554");
        assertThat(firstBody.get("account_id").asLong()).isNotEqualTo(secondBody.get("account_id").asLong());
    }

    private ResponseEntity<String> postJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/accounts", new HttpEntity<>(json, headers), String.class);
    }
}
