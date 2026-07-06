package io.github.danmke.transactions.application;

import io.github.danmke.transactions.domain.Account;
import io.github.danmke.transactions.domain.Transaction;
import io.github.danmke.transactions.exception.AccountNotFoundException;
import io.github.danmke.transactions.exception.InvalidOperationTypeException;
import io.github.danmke.transactions.exception.InvalidTransactionAmountException;
import io.github.danmke.transactions.exception.NegativeAmountNotAllowedException;
import io.github.danmke.transactions.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    AccountService accountService;

    TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, accountService, FIXED_CLOCK);
    }

    @Test
    void createsTransactionAfterValidatingDependencies() {
        when(accountService.getById(1L)).thenReturn(new Account("12345678900"));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = transactionService.create(1L, 1, new BigDecimal("123.45"));

        assertThat(transaction.getAccountId()).isEqualTo(1L);
        assertThat(transaction.getOperationType().getId()).isEqualTo(1);
        assertThat(transaction.getAmount()).isEqualByComparingTo("-123.45");
        assertThat(transaction.getEventDate().toInstant()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void rejectsNegativeAmountBeforeAccountLookup() {
        assertThatThrownBy(() -> transactionService.create(999L, 1, new BigDecimal("-10.00")))
                .isInstanceOf(NegativeAmountNotAllowedException.class);

        verifyNoInteractions(accountService, transactionRepository);
    }

    @Test
    void rejectsMissingAccountBeforeOperationTypeValidation() {
        when(accountService.getById(999L)).thenThrow(new AccountNotFoundException(999L));

        assertThatThrownBy(() -> transactionService.create(999L, 99, new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountService).getById(999L);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rejectsInvalidOperationTypeBeforeZeroAmountValidation() {
        when(accountService.getById(1L)).thenReturn(new Account("12345678900"));

        assertThatThrownBy(() -> transactionService.create(1L, 99, BigDecimal.ZERO))
                .isInstanceOf(InvalidOperationTypeException.class);

        verify(accountService).getById(1L);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void rejectsZeroAmountAfterAccountAndOperationTypeValidation() {
        when(accountService.getById(1L)).thenReturn(new Account("12345678900"));

        assertThatThrownBy(() -> transactionService.create(1L, 1, BigDecimal.ZERO))
                .isInstanceOf(InvalidTransactionAmountException.class);

        verify(accountService).getById(1L);
        verifyNoInteractions(transactionRepository);
    }
}
