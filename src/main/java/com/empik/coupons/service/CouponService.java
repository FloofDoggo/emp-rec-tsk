package com.empik.coupons.service;

import com.empik.coupons.entity.Coupon;
import com.empik.coupons.exception.CouponAlreadyExistsException;
import com.empik.coupons.exception.CouponNotFoundException;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.valueobject.CouponCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class CouponService {
    private static final Logger log = LoggerFactory.getLogger(CouponService.class);
    private final CouponRepository coupons;
    private final Clock clock;

    public CouponService(CouponRepository coupons, Clock clock) {
        this.coupons = coupons;
        this.clock = clock;
    }

    public Coupon create(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
                UUID.randomUUID(),
                command.code(),
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                command.maxUses(),
                command.country());

        if (!coupons.insertIfAbsent(coupon)) {
            throw new CouponAlreadyExistsException(coupon.code());
        }

        log.info("Coupon created: code={}, maxUses={}, country={}", coupon.code(), coupon.maxUses(), coupon.country());
        return coupon;
    }

    public Coupon get(CouponCode code) {
        return coupons.findByCode(code).orElseThrow(() -> new CouponNotFoundException(code));
    }
}