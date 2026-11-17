package com.empik.coupons.repository;

import com.empik.coupons.valueobject.UserId;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
public class PostgresJdbcRedemptionRepository implements RedemptionRepository {
    private final JdbcClient jdbc;

    public PostgresJdbcRedemptionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(UUID couponId, UserId userId, Instant redeemedAt) {
        int inserted = jdbc.sql("""
                        INSERT INTO coupon_redemptions (coupon_id, user_id, redeemed_at)
                        VALUES (:couponId, :userId, :redeemedAt)
                        ON CONFLICT (coupon_id, user_id) DO NOTHING
                        """)
                .param("couponId", couponId)
                .param("userId", userId.value())
                .param("redeemedAt", OffsetDateTime.ofInstant(redeemedAt, ZoneOffset.UTC))
                .update();
        return inserted == 1;
    }
}