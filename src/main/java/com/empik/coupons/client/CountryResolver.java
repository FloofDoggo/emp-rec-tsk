package com.empik.coupons.client;

import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.IpAddress;
import java.util.Optional;

public interface CountryResolver {
    Optional<CountryCode> resolve(IpAddress address);
}