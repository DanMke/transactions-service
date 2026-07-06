package io.github.danmke.transactions.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationTypeTest {

    @ParameterizedTest
    @CsvSource({
        // purchases and withdrawal must become negative, voucher positive
        "NORMAL_PURCHASE,      123.45, -123.45",
        "INSTALLMENT_PURCHASE, 100.00, -100.00",
        "WITHDRAWAL,            50.00,  -50.00",
        "CREDIT_VOUCHER,        60.00,   60.00",
        // sign is always derived from the absolute value, regardless of input sign
        "NORMAL_PURCHASE,     -123.45, -123.45",
        "CREDIT_VOUCHER,       -60.00,   60.00"
    })
    void normalizeAppliesCorrectSign(OperationType type, BigDecimal input, BigDecimal expected) {
        assertThat(type.normalize(input)).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "1, NORMAL_PURCHASE",
        "2, INSTALLMENT_PURCHASE",
        "3, WITHDRAWAL",
        "4, CREDIT_VOUCHER"
    })
    void fromIdResolvesSupportedOperationTypes(int id, OperationType expected) {
        assertThat(OperationType.fromId(id)).isEqualTo(expected);
    }

    @Test
    void fromIdRejectsUnknownOperationType() {
        assertThatThrownBy(() -> OperationType.fromId(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown operation type id: 99");
    }
}
