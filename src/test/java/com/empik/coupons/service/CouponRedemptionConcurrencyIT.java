package com.empik.coupons.service;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.exception.CouponAlreadyExistsException;
import com.empik.coupons.exception.CouponAlreadyRedeemedException;
import com.empik.coupons.exception.CouponExhaustedException;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import com.empik.coupons.valueobject.IpAddress;
import com.empik.coupons.valueobject.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The core promise of the service under real contention: "first come, first served" up to the limit, never above it,
 * and a user can never redeem twice - verified against PostgreSQL with many simultaneous transactions.
 */
class CouponRedemptionConcurrencyIT extends AbstractIntegrationTest {

    private static final IpAddress IP = IpAddress.of("8.8.8.8");

    @Autowired
    CouponService couponService;

    @Autowired
    CouponRedemptionService redemptionService;

    private enum Outcome { REDEEMED, EXHAUSTED, ALREADY_REDEEMED }

    @Test
    void neverExceedsMaxUsesWhenManyDifferentUsersRaceForTheLastCoupons() throws Exception {
        int maxUses = 10;
        int contenders = 60;
        CouponCode code = CouponCode.of("RACE");
        couponService.create(new CreateCouponCommand(code, maxUses, CountryCode.of("PL")));

        List<Outcome> outcomes = runConcurrently(contenders, i -> redeemAs(code, "user-" + i));

        assertThat(count(outcomes, Outcome.REDEEMED)).isEqualTo(maxUses);
        assertThat(count(outcomes, Outcome.EXHAUSTED)).isEqualTo(contenders - maxUses);
        assertThat(couponService.get(code).currentUses()).isEqualTo(maxUses);
        // The counter and the redemption log must agree: atomicity of the two-statement transaction.
        assertThat(redemptionRows(code)).isEqualTo(maxUses);
    }

    @Test
    void sameUserRacingWithItselfRedeemsExactlyOnce() throws Exception {
        CouponCode code = CouponCode.of("SOLO");
        couponService.create(new CreateCouponCommand(code, 100, CountryCode.of("PL")));

        List<Outcome> outcomes = runConcurrently(30, i -> redeemAs(code, "the-same-user"));

        assertThat(count(outcomes, Outcome.REDEEMED)).isEqualTo(1);
        assertThat(count(outcomes, Outcome.ALREADY_REDEEMED)).isEqualTo(29);
        assertThat(couponService.get(code).currentUses()).isEqualTo(1);
        assertThat(redemptionRows(code)).isEqualTo(1);
    }

    @Test
    void concurrentCreationOfTheSameCodeInDifferentCaseYieldsASingleCoupon() throws Exception {
        List<Boolean> created = runConcurrently(20, i -> {
            try {
                couponService.create(new CreateCouponCommand(
                        CouponCode.of(i % 2 == 0 ? "WIOSNA" : "wiosna"), 5, CountryCode.of("PL")));
                return true;
            } catch (CouponAlreadyExistsException e) {
                return false;
            }
        });

        assertThat(created.stream().filter(Boolean::booleanValue)).hasSize(1);
        assertThat(jdbc.sql("SELECT count(*) FROM coupons").query(Long.class).single()).isEqualTo(1L);
    }

    private Outcome redeemAs(CouponCode code, String user) {
        try {
            redemptionService.redeem(new RedeemCouponCommand(code, UserId.of(user), IP));
            return Outcome.REDEEMED;
        } catch (CouponExhaustedException e) {
            return Outcome.EXHAUSTED;
        } catch (CouponAlreadyRedeemedException e) {
            return Outcome.ALREADY_REDEEMED;
        }
    }

    private long redemptionRows(CouponCode code) {
        return jdbc.sql("""
                        SELECT count(*) FROM coupon_redemptions r JOIN coupons c ON c.id = r.coupon_id
                        WHERE c.code = :code
                        """)
                .param("code", code.value())
                .query(Long.class).single();
    }

    private static long count(List<Outcome> outcomes, Outcome wanted) {
        return outcomes.stream().filter(o -> o == wanted).count();
    }

    /** Releases all tasks at the same instant to maximise contention. */
    private static <T> List<T> runConcurrently(int threads, IntFunction<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<T> callable = () -> {
                    ready.countDown();
                    go.await();
                    return task.apply(index);
                };
                futures.add(pool.submit(callable));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }
}