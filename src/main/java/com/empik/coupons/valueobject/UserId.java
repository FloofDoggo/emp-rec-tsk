package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;

public record UserId(String value) {
    public static final int MAX_LENGTH = 128;

    public UserId {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("User id must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidInputException("User id must not be longer than %d characters".formatted(MAX_LENGTH));
        }
    }

    public static UserId of(String raw) {
        return new UserId(raw);
    }
}