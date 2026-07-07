package io.github.danmke.transactions.application;

import io.github.danmke.transactions.domain.OperationType;
import io.github.danmke.transactions.domain.Transaction;
import io.github.danmke.transactions.exception.InvalidOperationTypeException;
import io.github.danmke.transactions.exception.InvalidTransactionAmountException;
import io.github.danmke.transactions.exception.NegativeAmountNotAllowedException;
import io.github.danmke.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService,
                              Clock clock) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.clock = clock;
    }

    @Transactional
    public Transaction create(Long accountId, Integer operationTypeId, BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new NegativeAmountNotAllowedException(amount);
        }

        accountService.getById(accountId);
        OperationType operationType = resolveOperationType(operationTypeId);

        if (amount.signum() == 0) {
            throw new InvalidTransactionAmountException();
        }

        Transaction transaction = new Transaction(
                accountId, operationType, amount, OffsetDateTime.now(clock));
        return transactionRepository.save(transaction);
    }

    private OperationType resolveOperationType(Integer operationTypeId) {
        try {
            return OperationType.fromId(operationTypeId);
        } catch (IllegalArgumentException ex) {
            throw new InvalidOperationTypeException(operationTypeId);
        }
    }
}
