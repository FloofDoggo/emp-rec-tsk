package com.empik.coupons.service;

import com.empik.coupons.entity.Coupon;
import com.empik.coupons.exception.CouponAlreadyExistsException;
import com.empik.coupons.exception.CouponNotFoundException;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:15:30.123456789Z");

    @Mock
    CouponRepository coupons;

    private CouponService service() {
        return new CouponService(coupons, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsCouponWithZeroUsesAndNormalisedCode() {
        when(coupons.insertIfAbsent(any())).thenReturn(true);

        Coupon created = service().create(new CreateCouponCommand(CouponCode.of("wiosna"), 5, CountryCode.of("PL")));

        ArgumentCaptor<Coupon> saved = ArgumentCaptor.forClass(Coupon.class);
        verify(coupons).insertIfAbsent(saved.capture());
        assertThat(saved.getValue()).isEqualTo(created);
        assertThat(created.code().value()).isEqualTo("WIOSNA");
        assertThat(created.currentUses()).isZero();
        assertThat(created.maxUses()).isEqualTo(5);
        assertThat(created.createdAt()).isEqualTo(Instant.parse("2026-01-15T10:15:30.123456Z"));
    }

    @Test
    void rejectsDuplicateCode() {
        when(coupons.insertIfAbsent(any())).thenReturn(false);

        assertThatThrownBy(() -> service().create(new CreateCouponCommand(CouponCode.of("WIOSNA"), 5, CountryCode.of("PL"))))
                .isInstanceOf(CouponAlreadyExistsException.class);
    }

    @Test
    void getFailsForUnknownCoupon() {
        when(coupons.findByCode(CouponCode.of("NOPE"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(CouponCode.of("nope"))).isInstanceOf(CouponNotFoundException.class);
    }
}