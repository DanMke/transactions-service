package io.github.danmke.transactions.application;

import io.github.danmke.transactions.domain.Account;
import io.github.danmke.transactions.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account create(String documentNumber) {
        return accountRepository.save(new Account(documentNumber));
    }
}
