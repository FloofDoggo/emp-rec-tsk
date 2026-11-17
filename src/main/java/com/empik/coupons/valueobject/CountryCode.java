package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record CountryCode(String value) {
    private static final Pattern SHAPE = Pattern.compile("^[A-Za-z]{2}$");
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public CountryCode {
        if (value == null) {
            throw new InvalidInputException("Country code must not be null");
        }
        String trimmed = value.strip();
        if (!SHAPE.matcher(trimmed).matches()) {
            throw new InvalidInputException("Country must be a two-letter ISO 3166-1 alpha-2 code");
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(normalized)) {
            throw new InvalidInputException("Unknown country code '%s'".formatted(normalized));
        }
        value = normalized;
    }

    public static CountryCode of(String raw) {
        return new CountryCode(raw);
    }
}