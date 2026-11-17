package com.empik.coupons.exception;

import com.empik.coupons.valueobject.CouponCode;

public final class CouponNotFoundException extends CouponException {
    public CouponNotFoundException(CouponCode code) {
        super(ErrorCode.COUPON_NOT_FOUND, "Coupon '%s' does not exist".formatted(code.value()));
    }
}