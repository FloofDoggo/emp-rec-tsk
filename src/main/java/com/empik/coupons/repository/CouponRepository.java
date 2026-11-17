package com.empik.coupons.repository;

import com.empik.coupons.entity.Coupon;
import com.empik.coupons.valueobject.CouponCode;
import java.util.Optional;
import java.util.UUID;

public interface CouponRepository {
    boolean insertIfAbsent(Coupon coupon);
    Optional<Coupon> findByCode(CouponCode code);
    boolean incrementUsesIfAvailable(UUID couponId);
}