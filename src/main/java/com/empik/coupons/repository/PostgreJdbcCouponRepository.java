package com.empik.coupons.repository;

import com.empik.coupons.entity.Coupon;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PostgreJdbcCouponRepository implements CouponRepository {
    private final JdbcClient jdbc;
    public PostgreJdbcCouponRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(Coupon coupon) {
        int inserted = jdbc.sql("""
                        INSERT INTO coupons (id, code, created_at, max_uses, current_uses, country)
                        VALUES (:id, :code, :createdAt, :maxUses, :currentUses, :country)
                        ON CONFLICT (code) DO NOTHING
                        """)
                .param("id", coupon.id())
                .param("code", coupon.code().value())
                .param("createdAt", OffsetDateTime.ofInstant(coupon.createdAt(), ZoneOffset.UTC))
                .param("maxUses", coupon.maxUses())
                .param("currentUses", coupon.currentUses())
                .param("country", coupon.country().value())
                .update();
        return inserted == 1;
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jdbc.sql("""
                        SELECT id, code, created_at, max_uses, current_uses, country
                        FROM coupons
                        WHERE code = :code
                        """)
                .param("code", code.value())
                .query(PostgreJdbcCouponRepository::mapRow)
                .optional();
    }

    @Override
    public boolean incrementUsesIfAvailable(UUID couponId) {
        int updated = jdbc.sql("""
                        UPDATE coupons
                        SET current_uses = current_uses + 1
                        WHERE id = :id AND current_uses < max_uses
                        """)
                .param("id", couponId)
                .update();
        return updated == 1;
    }

    private static Coupon mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Coupon(
                rs.getObject("id", UUID.class),
                CouponCode.of(rs.getString("code")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getInt("max_uses"),
                rs.getInt("current_uses"),
                CountryCode.of(rs.getString("country")));
    }
}