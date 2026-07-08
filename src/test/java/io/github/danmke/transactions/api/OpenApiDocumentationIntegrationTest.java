package io.github.danmke.transactions.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.danmke.transactions.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void documentsAllApiEndpointsWithTheirStatusCodes() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode apiDocs = objectMapper.readTree(response.getBody());

        JsonNode paths = apiDocs.get("paths");
        assertThat(paths.has("/accounts")).isTrue();
        assertThat(paths.has("/accounts/{accountId}")).isTrue();
        assertThat(paths.has("/transactions")).isTrue();
        assertThat(paths.has("/transactions/{transactionId}")).isTrue();

        JsonNode createAccountOperation = paths.get("/accounts").get("post");
        assertRequestBodyDocumentsSchemaAndExample(createAccountOperation, "CreateAccountRequest");
        assertResponseDocumentsSchemaAndExample(createAccountOperation, "201", "AccountResponse");
        assertProblemDetailResponse(createAccountOperation, "400");

        JsonNode getAccountOperation = paths.get("/accounts/{accountId}").get("get");
        assertResponseDocumentsSchemaAndExample(getAccountOperation, "200", "AccountResponse");
        assertProblemDetailResponse(getAccountOperation, "400");
        assertProblemDetailResponse(getAccountOperation, "404");

        JsonNode createTransactionOperation = paths.get("/transactions").get("post");
        assertRequestBodyDocumentsSchemaAndExample(createTransactionOperation, "CreateTransactionRequest");
        assertResponseDocumentsSchemaAndExample(createTransactionOperation, "201", "TransactionResponse");
        assertProblemDetailResponse(createTransactionOperation, "400");
        assertProblemDetailResponse(createTransactionOperation, "404");
        assertProblemDetailResponse(createTransactionOperation, "422");

        JsonNode listTransactionsOperation = paths.get("/transactions").get("get");
        assertArrayResponseDocumentsSchemaAndExample(listTransactionsOperation, "200", "TransactionResponse");
        assertProblemDetailResponse(listTransactionsOperation, "400");
        assertProblemDetailResponse(listTransactionsOperation, "404");

        JsonNode getTransactionOperation = paths.get("/transactions/{transactionId}").get("get");
        assertResponseDocumentsSchemaAndExample(getTransactionOperation, "200", "TransactionResponse");
        assertProblemDetailResponse(getTransactionOperation, "400");
        assertProblemDetailResponse(getTransactionOperation, "404");

        JsonNode schemas = apiDocs.get("components").get("schemas");
        assertThat(schemas.has("CreateAccountRequest")).isTrue();
        assertThat(schemas.has("AccountResponse")).isTrue();
        assertThat(schemas.has("CreateTransactionRequest")).isTrue();
        assertThat(schemas.has("TransactionResponse")).isTrue();
    }

    @Test
    void swaggerUiIsReachable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/swagger-ui.html", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is3xxRedirection())
                .isTrue();
    }

    @Test
    void actuatorHealthStaysAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static void assertRequestBodyDocumentsSchemaAndExample(JsonNode operation, String schemaName) {
        JsonNode content = operation.path("requestBody").path("content").path("application/json");

        assertThat(schemaRef(content.path("schema"))).isEqualTo("#/components/schemas/" + schemaName);
        assertThat(content.path("examples").isObject()).isTrue();
        assertThat(content.path("examples").size()).isPositive();
    }

    private static void assertResponseDocumentsSchemaAndExample(
            JsonNode operation, String statusCode, String schemaName) {
        JsonNode content = responseContent(operation, statusCode);

        assertThat(schemaRef(content.path("schema"))).isEqualTo("#/components/schemas/" + schemaName);
        assertThat(content.path("examples").isObject()).isTrue();
        assertThat(content.path("examples").size()).isPositive();
    }

    private static void assertArrayResponseDocumentsSchemaAndExample(
            JsonNode operation, String statusCode, String schemaName) {
        JsonNode content = responseContent(operation, statusCode);

        assertThat(content.path("schema").path("type").asText()).isEqualTo("array");
        assertThat(schemaRef(content.path("schema").path("items"))).isEqualTo("#/components/schemas/" + schemaName);
        assertThat(content.path("examples").isObject()).isTrue();
        assertThat(content.path("examples").size()).isPositive();
    }

    private static void assertProblemDetailResponse(JsonNode operation, String statusCode) {
        JsonNode content = responseContent(operation, statusCode);

        assertThat(schemaRef(content.path("schema"))).isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(content.path("examples").isObject()).isTrue();
        assertThat(content.path("examples").size()).isPositive();
    }

    private static JsonNode responseContent(JsonNode operation, String statusCode) {
        JsonNode responseContent = operation.path("responses").path(statusCode).path("content");
        JsonNode content = responseContent.path("application/json");
        if (content.isMissingNode()) {
            content = responseContent.path("application/problem+json");
        }
        if (content.isMissingNode()) {
            content = responseContent.path("*/*");
        }
        assertThat(content.isObject()).isTrue();
        return content;
    }

    private static String schemaRef(JsonNode schema) {
        if (schema.has("$ref")) {
            return schema.get("$ref").asText();
        }
        return schema.path("allOf").path(0).path("$ref").asText();
    }
}
