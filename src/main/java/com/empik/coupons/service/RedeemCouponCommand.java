package com.empik.coupons.service;

import com.empik.coupons.valueobject.CouponCode;
import com.empik.coupons.valueobject.IpAddress;
import com.empik.coupons.valueobject.UserId;

public record RedeemCouponCommand(CouponCode code, UserId userId, IpAddress clientIp) {}