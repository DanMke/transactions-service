package io.github.danmke.transactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danmke.transactions.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsAccountAndReadsItBack() throws Exception {
        ResponseEntity<String> created = postAccount("{\"document_number\": \"12345678900\"}");

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdBody = objectMapper.readTree(created.getBody());
        long accountId = createdBody.get("account_id").asLong();
        assertThat(accountId).isPositive();
        assertThat(createdBody.get("document_number").asText()).isEqualTo("12345678900");
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getHeaders().getLocation().getPath()).isEqualTo("/accounts/" + accountId);

        ResponseEntity<String> fetched = restTemplate.getForEntity("/accounts/" + accountId, String.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode fetchedBody = objectMapper.readTree(fetched.getBody());
        assertThat(fetchedBody.get("account_id").asLong()).isEqualTo(accountId);
        assertThat(fetchedBody.get("document_number").asText()).isEqualTo("12345678900");
    }

    @Test
    void allowsSameDocumentNumberForDifferentAccounts() throws Exception {
        String json = "{\"document_number\": \"99887766554\"}";

        ResponseEntity<String> first = postAccount(json);
        ResponseEntity<String> second = postAccount(json);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long firstId = objectMapper.readTree(first.getBody()).get("account_id").asLong();
        long secondId = objectMapper.readTree(second.getBody()).get("account_id").asLong();
        assertThat(firstId).isNotEqualTo(secondId);
    }

    @Test
    void returnsProblemDetail404OverTheWireWhenAccountNotFound() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/accounts/99999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType())
                .matches(type -> type.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("type").asText()).isEqualTo("urn:problem-type:account-not-found");
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("detail").asText()).contains("99999999");
    }

    private ResponseEntity<String> postAccount(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/accounts", new HttpEntity<>(json, headers), String.class);
    }
}
