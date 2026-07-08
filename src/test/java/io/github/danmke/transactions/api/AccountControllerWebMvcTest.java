package io.github.danmke.transactions.api;

import io.github.danmke.transactions.application.AccountService;
import io.github.danmke.transactions.domain.Account;
import io.github.danmke.transactions.exception.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountService accountService;

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(1L);
        when(account.getDocumentNumber()).thenReturn("12345678900");
        when(accountService.create("12345678900")).thenReturn(account);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\": \"12345678900\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/accounts/1")))
                .andExpect(jsonPath("$.account_id").value(1))
                .andExpect(jsonPath("$.document_number").value("12345678900"));
    }

    @Test
    void rejectsMissingDocumentNumber() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"))
                .andExpect(jsonPath("$.errors[0].field").value("document_number"));
    }

    @Test
    void rejectsBlankDocumentNumber() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void rejectsDocumentNumberExceedingMaxSize() throws Exception {
        String tooLong = "1".repeat(51);

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-failed"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document_number\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"));
    }

    @Test
    void returns404WhenAccountMissing() throws Exception {
        when(accountService.getById(99999999L)).thenThrow(new AccountNotFoundException(99999999L));

        mockMvc.perform(get("/accounts/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:account-not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returns400ForNonNumericAccountId() throws Exception {
        mockMvc.perform(get("/accounts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-request-parameter"));
    }
}
