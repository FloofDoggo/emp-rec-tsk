package com.empik.coupons.exception;

public final class InvalidInputException extends CouponException {
    public InvalidInputException(String message) {
        super(ErrorCode.INVALID_INPUT, message);
    }
}