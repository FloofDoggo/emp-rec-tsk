package com.empik.coupons.service;

import com.empik.coupons.client.CountryResolver;
import com.empik.coupons.entity.Coupon;
import com.empik.coupons.entity.Redemption;
import com.empik.coupons.exception.CountryNotAllowedException;
import com.empik.coupons.exception.CouponAlreadyRedeemedException;
import com.empik.coupons.exception.CouponExhaustedException;
import com.empik.coupons.exception.CouponNotFoundException;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.RedemptionRepository;
import com.empik.coupons.valueobject.CountryCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CouponRedemptionService {
    private static final Logger log = LoggerFactory.getLogger(CouponRedemptionService.class);
    private final CouponRepository coupons;
    private final RedemptionRepository redemptions;
    private final CountryResolver countryResolver;
    private final TransactionOperations transaction;
    private final Clock clock;

    public CouponRedemptionService(CouponRepository coupons,
                                   RedemptionRepository redemptions,
                                   CountryResolver countryResolver,
                                   TransactionOperations transaction,
                                   Clock clock) {
        this.coupons = coupons;
        this.redemptions = redemptions;
        this.countryResolver = countryResolver;
        this.transaction = transaction;
        this.clock = clock;
    }

    public Redemption redeem(RedeemCouponCommand command) {
        Coupon coupon = coupons.findByCode(command.code())
                .orElseThrow(() -> new CouponNotFoundException(command.code()));
        assertCallerCountryAllowed(coupon, command);

        if (coupon.isExhausted()) {
            throw new CouponExhaustedException(coupon.code());
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        transaction.executeWithoutResult(status -> {
            if (!redemptions.insertIfAbsent(coupon.id(), command.userId(), now)) {
                throw new CouponAlreadyRedeemedException(coupon.code());
            }
            if (!coupons.incrementUsesIfAvailable(coupon.id())) {
                throw new CouponExhaustedException(coupon.code());
            }
        });

        log.info("Coupon redeemed: code={}, userId={}", coupon.code(), command.userId());
        return new Redemption(coupon.code(), command.userId(), now);
    }

    private void assertCallerCountryAllowed(Coupon coupon, RedeemCouponCommand command) {
        CountryCode callerCountry = countryResolver.resolve(command.clientIp()).orElse(null);
        if (!coupon.isAllowedIn(callerCountry)) {
            log.info("Coupon redemption denied by country: code={}, couponCountry={}, callerCountry={}",
                    coupon.code(), coupon.country(), callerCountry);
            throw new CountryNotAllowedException(coupon.code());
        }
    }
}