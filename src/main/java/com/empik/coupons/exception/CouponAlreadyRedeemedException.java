package com.empik.coupons.exception;

import com.empik.coupons.valueobject.CouponCode;

public final class CouponAlreadyRedeemedException extends CouponException {
    public CouponAlreadyRedeemedException(CouponCode code) {
        super(ErrorCode.COUPON_ALREADY_REDEEMED,
                "Coupon '%s' has already been redeemed by this user".formatted(code.value()));
    }
}