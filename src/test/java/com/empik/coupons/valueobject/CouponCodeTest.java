package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @Test
    void codesDifferingOnlyInCaseAreEqual() {
        assertThat(CouponCode.of("WIOSNA")).isEqualTo(CouponCode.of("wiosna"));
        assertThat(CouponCode.of("wIoSnA").value()).isEqualTo("WIOSNA");
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertThat(CouponCode.of("  summer-24 ").value()).isEqualTo("SUMMER-24");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ab", "has space", "semi;colon", "zażółć", "wıosna", "Kelvin"})
    void invalidCodesAreRejected(String raw) {
        assertThatThrownBy(() -> CouponCode.of(raw)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void nullIsRejected() {
        assertThatThrownBy(() -> CouponCode.of(null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void tooLongCodeIsRejected() {
        assertThatThrownBy(() -> CouponCode.of("A".repeat(65))).isInstanceOf(InvalidInputException.class);
    }
}
