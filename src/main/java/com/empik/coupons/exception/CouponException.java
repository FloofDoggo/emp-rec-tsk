package com.empik.coupons.exception;

public abstract sealed class CouponException extends RuntimeException
        permits CountryNotAllowedException, CouponAlreadyExistsException, CouponAlreadyRedeemedException,
        CouponExhaustedException, CouponNotFoundException, GeolocationUnavailableException, InvalidInputException{
    private final ErrorCode errorCode;

    protected CouponException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}