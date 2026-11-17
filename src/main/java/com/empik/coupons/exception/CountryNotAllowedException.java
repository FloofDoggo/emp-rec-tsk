package com.empik.coupons.exception;

import com.empik.coupons.valueobject.CouponCode;

public final class CountryNotAllowedException extends CouponException {
    public CountryNotAllowedException(CouponCode code) {
        super(ErrorCode.COUNTRY_NOT_ALLOWED,
                "Coupon '%s' cannot be redeemed from your country".formatted(code.value()));
    }
}