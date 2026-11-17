package com.empik.coupons.service;

import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;

public record CreateCouponCommand(CouponCode code, int maxUses, CountryCode country) {}