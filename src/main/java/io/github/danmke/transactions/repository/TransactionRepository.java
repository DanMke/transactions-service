package io.github.danmke.transactions.repository;

import io.github.danmke.transactions.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
