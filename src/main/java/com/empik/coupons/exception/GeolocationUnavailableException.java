package com.empik.coupons.exception;

public final class GeolocationUnavailableException extends CouponException {
    public GeolocationUnavailableException(String message) {
        super(ErrorCode.GEOLOCATION_UNAVAILABLE, message);
    }
}