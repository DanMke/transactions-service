package io.github.danmke.transactions.application;

import io.github.danmke.transactions.domain.OperationType;
import io.github.danmke.transactions.domain.Transaction;
import io.github.danmke.transactions.exception.AccountNotFoundException;
import io.github.danmke.transactions.exception.InvalidOperationTypeException;
import io.github.danmke.transactions.exception.InvalidTransactionAmountException;
import io.github.danmke.transactions.exception.NegativeAmountNotAllowedException;
import io.github.danmke.transactions.exception.TransactionNotFoundException;
import io.github.danmke.transactions.observability.BusinessMetrics;
import io.github.danmke.transactions.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final Clock clock;
    private final BusinessMetrics businessMetrics;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountService accountService,
                              Clock clock,
                              BusinessMetrics businessMetrics) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.clock = clock;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public Transaction create(Long accountId, Integer operationTypeId, BigDecimal amount) {
        if (amount.signum() < 0) {
            businessMetrics.transactionFailed("negative_amount");
            throw new NegativeAmountNotAllowedException(amount);
        }

        try {
            accountService.getById(accountId);
        } catch (AccountNotFoundException ex) {
            businessMetrics.transactionFailed("account_not_found");
            throw ex;
        }

        OperationType operationType = resolveOperationType(operationTypeId);

        if (amount.signum() == 0) {
            businessMetrics.transactionFailed("zero_amount");
            throw new InvalidTransactionAmountException();
        }

        Transaction transaction = new Transaction(
                accountId, operationType, amount, OffsetDateTime.now(clock));
        Transaction savedTransaction = transactionRepository.save(transaction);
        businessMetrics.transactionCreated(operationType);
        return savedTransaction;
    }

    @Transactional(readOnly = true)
    public Transaction getById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> listByAccountId(Long accountId) {
        accountService.getById(accountId);
        return transactionRepository.findByAccountIdOrderByIdDesc(accountId);
    }

    private OperationType resolveOperationType(Integer operationTypeId) {
        try {
            return OperationType.fromId(operationTypeId);
        } catch (IllegalArgumentException ex) {
            businessMetrics.transactionFailed("invalid_operation_type");
            throw new InvalidOperationTypeException(operationTypeId);
        }
    }
}
