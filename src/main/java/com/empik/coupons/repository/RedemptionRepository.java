package com.empik.coupons.repository;

import com.empik.coupons.valueobject.UserId;
import java.time.Instant;
import java.util.UUID;

public interface RedemptionRepository {
    boolean insertIfAbsent(UUID couponId, UserId userId, Instant redeemedAt);
}