package io.github.danmke.transactions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    protected Account() {
        // required by JPA
    }

    public Account(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("Document number must not be blank");
        }

        this.documentNumber = documentNumber.trim();
    }

    public Long getId() {
        return id;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }
}
