package io.github.danmke.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void prometheusExposesBusinessMetrics() throws Exception {
        long accountId = createAccount();

        postJson("/transactions", "{\"account_id\": " + accountId
                + ", \"operation_type_id\": 1, \"amount\": 10.00}");
        postJson("/transactions", "{\"account_id\": " + accountId
                + ", \"operation_type_id\": 1, \"amount\": 0.00}");

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody())
                .contains("accounts_creation_total")
                .contains("transactions_creation_total")
                .contains("operation_type=\"NORMAL_PURCHASE\"")
                .contains("transactions_failed_total")
                .contains("reason=\"zero_amount\"");
    }

    private long createAccount() throws Exception {
        ResponseEntity<String> response = postJson("/accounts", "{\"document_number\": \"10101010101\"}");
        return objectMapper.readTree(response.getBody()).get("account_id").asLong();
    }

    private ResponseEntity<String> postJson(String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(path, new HttpEntity<>(json, headers), String.class);
    }
}
