package io.github.danmke.transactions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Convert(converter = OperationTypeConverter.class)
    @Column(name = "operation_type_id", nullable = false)
    private OperationType operationType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "event_date", nullable = false)
    private OffsetDateTime eventDate;

    protected Transaction() {
        // required by JPA
    }

    public Transaction(Long accountId, OperationType operationType, BigDecimal amount, OffsetDateTime eventDate) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account id must not be null");
        }
        if (operationType == null) {
            throw new IllegalArgumentException("Operation type must not be null");
        }
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException("Amount must not be null or zero");
        }
        if (eventDate == null) {
            throw new IllegalArgumentException("Event date must not be null");
        }

        this.accountId = accountId;
        this.operationType = operationType;
        this.amount = operationType.normalize(amount);
        this.eventDate = eventDate;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public OffsetDateTime getEventDate() {
        return eventDate;
    }
}
