package com.empik.coupons.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCouponRequest(
        @NotBlank @Size(max = 128) String code,
        @NotNull @Positive Integer maxUses,
        @NotBlank @Size(max = 8) String country) {
}