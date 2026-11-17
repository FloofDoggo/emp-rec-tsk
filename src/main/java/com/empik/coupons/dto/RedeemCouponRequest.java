package com.empik.coupons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedeemCouponRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 256) String userId) {
}