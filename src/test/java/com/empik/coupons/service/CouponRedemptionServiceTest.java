package com.empik.coupons.service;

import com.empik.coupons.client.CountryResolver;
import com.empik.coupons.entity.Coupon;
import com.empik.coupons.entity.Redemption;
import com.empik.coupons.exception.CountryNotAllowedException;
import com.empik.coupons.exception.CouponAlreadyRedeemedException;
import com.empik.coupons.exception.CouponExhaustedException;
import com.empik.coupons.exception.CouponNotFoundException;
import com.empik.coupons.exception.GeolocationUnavailableException;
import com.empik.coupons.repository.CouponRepository;
import com.empik.coupons.repository.RedemptionRepository;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import com.empik.coupons.valueobject.IpAddress;
import com.empik.coupons.valueobject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponRedemptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:15:30Z");
    private static final CouponCode CODE = CouponCode.of("WIOSNA");
    private static final UserId USER = UserId.of("user-1");
    private static final IpAddress IP = IpAddress.of("8.8.8.8");
    private static final UUID COUPON_ID = UUID.randomUUID();

    @Mock CouponRepository coupons;
    @Mock RedemptionRepository redemptions;
    @Mock CountryResolver countryResolver;

    private CouponRedemptionService service;

    @BeforeEach
    void setUp() {
        // withoutTransaction(): just runs the callback; transactional behaviour is covered by the integration tests.
        service = new CouponRedemptionService(coupons, redemptions, countryResolver,
                TransactionOperations.withoutTransaction(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static Coupon coupon(int maxUses, int currentUses, String country) {
        return new Coupon(COUPON_ID, CODE, NOW, maxUses, currentUses, CountryCode.of(country));
    }

    private RedeemCouponCommand command() {
        return new RedeemCouponCommand(CODE, USER, IP);
    }

    @Test
    void redeemsCouponForUserFromAllowedCountry() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.of(CountryCode.of("PL")));
        when(redemptions.insertIfAbsent(COUPON_ID, USER, NOW)).thenReturn(true);
        when(coupons.incrementUsesIfAvailable(COUPON_ID)).thenReturn(true);

        Redemption result = service.redeem(command());

        assertThat(result).isEqualTo(new Redemption(CODE, USER, NOW));
    }

    @Test
    void failsForUnknownCouponWithoutCallingGeolocation() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CouponNotFoundException.class);

        verifyNoInteractions(countryResolver, redemptions);
    }

    @Test
    void failsForForeignCountryWithoutTouchingTheDatabase() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.of(CountryCode.of("DE")));

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CountryNotAllowedException.class);

        verifyNoInteractions(redemptions);
        verify(coupons, never()).incrementUsesIfAvailable(any());
    }

    @Test
    void failsWhenCountryCannotBeDeterminedForPrivateAddress() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CountryNotAllowedException.class);
    }

    @Test
    void propagatesGeolocationOutageAndConsumesNothing() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 0, "PL")));
        when(countryResolver.resolve(IP)).thenThrow(new GeolocationUnavailableException("down"));

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(GeolocationUnavailableException.class);

        verifyNoInteractions(redemptions);
    }

    @Test
    void failsFastWhenSnapshotShowsCouponAlreadyExhausted() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 5, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.of(CountryCode.of("PL")));

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CouponExhaustedException.class);

        verifyNoInteractions(redemptions);
    }

    @Test
    void failsWhenUserAlreadyRedeemedTheCoupon() {
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 1, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.of(CountryCode.of("PL")));
        when(redemptions.insertIfAbsent(COUPON_ID, USER, NOW)).thenReturn(false);

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CouponAlreadyRedeemedException.class);

        verify(coupons, never()).incrementUsesIfAvailable(any());
    }

    @Test
    void failsWhenLastUseWasTakenByAConcurrentCaller() {
        // Snapshot says 4/5, but somebody else grabbed the last use between our read and our write.
        when(coupons.findByCode(CODE)).thenReturn(Optional.of(coupon(5, 4, "PL")));
        when(countryResolver.resolve(IP)).thenReturn(Optional.of(CountryCode.of("PL")));
        when(redemptions.insertIfAbsent(COUPON_ID, USER, NOW)).thenReturn(true);
        when(coupons.incrementUsesIfAvailable(COUPON_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.redeem(command())).isInstanceOf(CouponExhaustedException.class);
    }
}