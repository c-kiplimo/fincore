package com.collins.platform.commons.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void addsSameCurrency() {
        assertThat(Money.of(1000, "KES").plus(Money.of(250, "KES"))).isEqualTo(Money.of(1250, "KES"));
    }

    @Test
    void refusesMixedCurrencies() {
        assertThatThrownBy(() -> Money.of(1, "KES").plus(Money.of(1, "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void knowsZeroAndThreeDecimalCurrencies() {
        assertThat(CurrencyUnits.toMinor(new BigDecimal("1500"), "JPY")).isEqualTo(1500);
        assertThat(CurrencyUnits.toMinor(new BigDecimal("1.234"), "KWD")).isEqualTo(1234);
    }

    @Test
    void roundsHalfEven() {
        assertThat(CurrencyUnits.applyRate(25, new BigDecimal("0.5"))).isEqualTo(12); // 12.5 -> 12
        assertThat(CurrencyUnits.applyRate(35, new BigDecimal("0.5"))).isEqualTo(18); // 17.5 -> 18
    }

    @Test
    void convertsBetweenCurrencies() {
        // USD 100.00 at 130 KES per USD = KES 13,000.00
        assertThat(CurrencyUnits.convert(10_000, "USD", new BigDecimal("130"), "KES")).isEqualTo(1_300_000);
    }
}
