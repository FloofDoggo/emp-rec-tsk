package com.empik.coupons.exception;

import com.empik.coupons.valueobject.CouponCode;

public final class CouponExhaustedException extends CouponException {
    public CouponExhaustedException(CouponCode code) {
        super(ErrorCode.COUPON_EXHAUSTED,
                "Coupon '%s' has reached its maximum number of uses".formatted(code.value()));
    }
}