package io.github.danmke.transactions.repository;

import io.github.danmke.transactions.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
