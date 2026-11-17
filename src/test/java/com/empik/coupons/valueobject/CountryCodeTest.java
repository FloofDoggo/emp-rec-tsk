package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountryCodeTest {

    @Test
    void isNormalisedToUpperCase() {
        assertThat(CountryCode.of(" pl ").value()).isEqualTo("PL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "P", "POL", "ZZ", "P1", "łł"})
    void invalidCountriesAreRejected(String raw) {
        assertThatThrownBy(() -> CountryCode.of(raw)).isInstanceOf(InvalidInputException.class);
    }
}
