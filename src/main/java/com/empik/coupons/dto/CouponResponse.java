package com.empik.coupons.dto;

import com.empik.coupons.entity.Coupon;
import java.time.Instant;

public record CouponResponse(String code, Instant createdAt, int maxUses, int currentUses, String country) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.code().value(),
                coupon.createdAt(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.country().value());
    }
}