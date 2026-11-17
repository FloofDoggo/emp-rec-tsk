package com.empik.coupons.dto;

import com.empik.coupons.entity.Redemption;
import java.time.Instant;

public record RedemptionResponse(String code, String userId, Instant redeemedAt) {
    public static RedemptionResponse from(Redemption redemption) {
        return new RedemptionResponse(
                redemption.code().value(),
                redemption.userId().value(),
                redemption.redeemedAt());
    }
}