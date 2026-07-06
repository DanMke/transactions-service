package io.github.danmke.transactions.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void createsAccountWithNormalizedDocumentNumber() {
        Account account = new Account(" 12345678900 ");

        assertThat(account.getDocumentNumber()).isEqualTo("12345678900");
    }

    @Test
    void rejectsBlankDocumentNumber() {
        assertThatThrownBy(() -> new Account("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document number must not be blank");
    }

    @Test
    void rejectsNullDocumentNumber() {
        assertThatThrownBy(() -> new Account(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document number must not be blank");
    }
}
