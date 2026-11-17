package com.empik.coupons.controller;

import com.empik.coupons.dto.RedeemCouponRequest;
import com.empik.coupons.dto.RedemptionResponse;
import com.empik.coupons.service.CouponRedemptionService;
import com.empik.coupons.service.RedeemCouponCommand;
import com.empik.coupons.valueobject.CouponCode;
import com.empik.coupons.valueobject.IpAddress;
import com.empik.coupons.valueobject.UserId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/redemptions")
class RedemptionController {
    private final CouponRedemptionService redemptionService;

    RedemptionController(CouponRedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RedemptionResponse redeem(@Valid @RequestBody RedeemCouponRequest request, HttpServletRequest httpRequest) {
        RedeemCouponCommand command = new RedeemCouponCommand(
                CouponCode.of(request.code()),
                UserId.of(request.userId()),
                IpAddress.of(httpRequest.getRemoteAddr()));
        return RedemptionResponse.from(redemptionService.redeem(command));
    }
}