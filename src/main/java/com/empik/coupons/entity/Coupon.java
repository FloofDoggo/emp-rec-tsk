package com.empik.coupons.entity;

import com.empik.coupons.exception.InvalidInputException;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import java.time.Instant;
import java.util.UUID;

public record Coupon(
        UUID id,
        CouponCode code,
        Instant createdAt,
        int maxUses,
        int currentUses,
        CountryCode country) {

    public Coupon {
        if (id == null || code == null || createdAt == null || country == null) {
            throw new IllegalArgumentException("Coupon fields must not be null");
        }
        if (maxUses < 1) {
            throw new InvalidInputException("Maximum number of uses must be at least 1");
        }
        if (currentUses < 0 || currentUses > maxUses) {
            throw new IllegalArgumentException("Current uses must be between 0 and maxUses");
        }
    }

    public static Coupon create(UUID id, CouponCode code, Instant createdAt, int maxUses, CountryCode country) {
        return new Coupon(id, code, createdAt, maxUses, 0, country);
    }

    public boolean isExhausted() {
        return currentUses >= maxUses;
    }

    public boolean isAllowedIn(CountryCode other) {
        return country.equals(other);
    }
}