package io.github.danmke.transactions.application;

import io.github.danmke.transactions.domain.Account;
import io.github.danmke.transactions.exception.AccountNotFoundException;
import io.github.danmke.transactions.observability.BusinessMetrics;
import io.github.danmke.transactions.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BusinessMetrics businessMetrics;

    public AccountService(AccountRepository accountRepository, BusinessMetrics businessMetrics) {
        this.accountRepository = accountRepository;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public Account create(String documentNumber) {
        Account account = accountRepository.save(new Account(documentNumber));
        businessMetrics.accountCreated();
        return account;
    }

    @Transactional(readOnly = true)
    public Account getById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
