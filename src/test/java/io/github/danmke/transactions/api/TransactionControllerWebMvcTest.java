package io.github.danmke.transactions.api;

import io.github.danmke.transactions.application.TransactionService;
import io.github.danmke.transactions.domain.OperationType;
import io.github.danmke.transactions.domain.Transaction;
import io.github.danmke.transactions.exception.AccountNotFoundException;
import io.github.danmke.transactions.exception.InvalidOperationTypeException;
import io.github.danmke.transactions.exception.InvalidTransactionAmountException;
import io.github.danmke.transactions.exception.NegativeAmountNotAllowedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TransactionService transactionService;

    @Test
    void createReturns201WithLocationAndNormalizedBody() throws Exception {
        Transaction transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(1L);
        when(transaction.getAccountId()).thenReturn(1L);
        when(transaction.getOperationType()).thenReturn(OperationType.NORMAL_PURCHASE);
        when(transaction.getAmount()).thenReturn(new BigDecimal("-123.45"));
        when(transaction.getEventDate()).thenReturn(OffsetDateTime.parse("2026-07-06T12:00:00Z"));
        when(transactionService.create(1L, 1, new BigDecimal("123.45"))).thenReturn(transaction);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 1, "123.45")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/transactions/1")))
                .andExpect(jsonPath("$.transaction_id").value(1))
                .andExpect(jsonPath("$.account_id").value(1))
                .andExpect(jsonPath("$.operation_type_id").value(1))
                .andExpect(jsonPath("$.amount").value(-123.45))
                .andExpect(jsonPath("$.event_date").exists());
    }

    @Test
    void rejectsMissingAccountId() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operation_type_id\": 1, \"amount\": 10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("account_id"));
    }

    @Test
    void rejectsMissingOperationTypeId() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id\": 1, \"amount\": 10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"));
    }

    @Test
    void rejectsMissingAmount() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id\": 1, \"operation_type_id\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"));
    }

    @Test
    void rejectsAmountWithTooManyFractionDigits() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 1, "10.123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"));
    }

    @Test
    void rejectsAmountWithTooManyIntegerDigits() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 1, "123456789012345678.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"));
    }

    @Test
    void rejectsNegativeAmountWith400() throws Exception {
        when(transactionService.create(anyLong(), anyInt(), any(BigDecimal.class)))
                .thenThrow(new NegativeAmountNotAllowedException(new BigDecimal("-10.00")));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 1, "-10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:negative-amount"));
    }

    @Test
    void returns404WhenAccountMissing() throws Exception {
        when(transactionService.create(anyLong(), anyInt(), any(BigDecimal.class)))
                .thenThrow(new AccountNotFoundException(99999999L));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(99999999L, 1, "10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:problem-type:account-not-found"));
    }

    @Test
    void returns422WhenOperationTypeUnknown() throws Exception {
        when(transactionService.create(anyLong(), anyInt(), any(BigDecimal.class)))
                .thenThrow(new InvalidOperationTypeException(99));

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 99, "10.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-operation-type"));
    }

    @Test
    void returns422WhenAmountZero() throws Exception {
        when(transactionService.create(anyLong(), anyInt(), any(BigDecimal.class)))
                .thenThrow(new InvalidTransactionAmountException());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(1L, 1, "0.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-transaction-amount"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id\": 1,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"));
    }

    private static String body(long accountId, int operationTypeId, String amount) {
        return "{\"account_id\": " + accountId
                + ", \"operation_type_id\": " + operationTypeId
                + ", \"amount\": " + amount + "}";
    }
}
