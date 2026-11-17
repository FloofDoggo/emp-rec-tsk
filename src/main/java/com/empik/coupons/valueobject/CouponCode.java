package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;
import java.util.Locale;
import java.util.regex.Pattern;

public record CouponCode(String value) {
    private static final Pattern ALLOWED = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");

    public CouponCode {
        if (value == null) {
            throw new InvalidInputException("Coupon code must not be null");
        }
        String trimmed = value.strip();

        if (!ALLOWED.matcher(trimmed).matches()) {
            throw new InvalidInputException(
                    "Coupon code must be 3-64 characters long and contain only letters A-Z, digits, '_' or '-'");
        }
        value = trimmed.toUpperCase(Locale.ROOT);
    }

    public static CouponCode of(String raw) {
        return new CouponCode(raw);
    }
}