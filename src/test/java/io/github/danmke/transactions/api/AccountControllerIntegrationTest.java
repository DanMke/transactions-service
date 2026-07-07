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
    void rejectsMissingDocumentNumber() throws Exception {
        ResponseEntity<String> response = postJson("{}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsBlankDocumentNumber() throws Exception {
        ResponseEntity<String> response = postJson("{\"document_number\": \"   \"}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsDocumentNumberExceedingMaxSize() throws Exception {
        String tooLong = "1".repeat(51);
        ResponseEntity<String> response = postJson("{\"document_number\": \"" + tooLong + "\"}");

        assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
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

    @Test
    void returnsAccountById() throws Exception {
        ResponseEntity<String> created = postJson("{\"document_number\": \"55544433322\"}");
        long accountId = objectMapper.readTree(created.getBody()).get("account_id").asLong();

        ResponseEntity<String> response = restTemplate.getForEntity("/accounts/" + accountId, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("account_id").asLong()).isEqualTo(accountId);
        assertThat(body.get("document_number").asText()).isEqualTo("55544433322");
    }

    @Test
    void returnsProblemDetail404WhenAccountNotFound() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/accounts/99999999", String.class);

        JsonNode body = assertProblemDetail(response, HttpStatus.NOT_FOUND);
        assertThat(body.get("title").asText()).isEqualTo("Account not found");
        assertThat(body.get("detail").asText()).contains("99999999");
    }

    @Test
    void validationErrorUsesProblemDetailFormat() throws Exception {
        ResponseEntity<String> response = postJson("{}");

        JsonNode body = assertValidationProblemDetail(response, HttpStatus.BAD_REQUEST);
        assertThat(body.get("title").asText()).isEqualTo("Validation failed");
        assertThat(body.get("errors").get(0).get("field").asText()).isEqualTo("document_number");
    }

    @Test
    void malformedJsonUsesProblemDetailFormat() throws Exception {
        ResponseEntity<String> response = postJson("{\"document_number\":");

        JsonNode body = assertProblemDetail(response, HttpStatus.BAD_REQUEST);
        assertThat(body.get("title").asText()).isEqualTo("Malformed JSON request");
    }

    @Test
    void invalidAccountIdPathVariableUsesProblemDetailFormat() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/accounts/abc", String.class);

        JsonNode body = assertProblemDetail(response, HttpStatus.BAD_REQUEST);
        assertThat(body.get("title").asText()).isEqualTo("Invalid request parameter");
    }

    private JsonNode assertValidationProblemDetail(ResponseEntity<String> response, HttpStatus expectedStatus)
            throws Exception {
        JsonNode body = assertProblemDetail(response, expectedStatus);
        assertThat(body.get("errors").isArray()).isTrue();
        assertThat(body.get("errors")).isNotEmpty();
        return body;
    }

    private JsonNode assertProblemDetail(ResponseEntity<String> response, HttpStatus expectedStatus) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType())
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("type").asText()).startsWith("urn:problem-type:");
        assertThat(body.get("title").asText()).isNotBlank();
        assertThat(body.get("status").asInt()).isEqualTo(expectedStatus.value());
        assertThat(body.get("detail").asText()).isNotBlank();
        return body;
    }

    private ResponseEntity<String> postJson(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/accounts", new HttpEntity<>(json, headers), String.class);
    }
}
