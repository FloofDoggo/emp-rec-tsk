package com.empik.coupons.exception;

import com.empik.coupons.valueobject.CouponCode;

public final class CouponAlreadyExistsException extends CouponException {
    public CouponAlreadyExistsException(CouponCode code) {
        super(ErrorCode.COUPON_ALREADY_EXISTS, "Coupon '%s' already exists".formatted(code.value()));
    }
}