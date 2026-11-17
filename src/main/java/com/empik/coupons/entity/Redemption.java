package com.empik.coupons.entity;

import com.empik.coupons.valueobject.CouponCode;
import com.empik.coupons.valueobject.UserId;
import java.time.Instant;

public record Redemption(CouponCode code, UserId userId, Instant redeemedAt) {}