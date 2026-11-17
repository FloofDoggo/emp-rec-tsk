package com.empik.coupons.controller;

import com.empik.coupons.dto.CouponResponse;
import com.empik.coupons.dto.CreateCouponRequest;
import com.empik.coupons.entity.Coupon;
import com.empik.coupons.service.CouponService;
import com.empik.coupons.service.CreateCouponCommand;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.CouponCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/coupons")
class CouponController {
    private final CouponService couponService;

    CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = couponService.create(new CreateCouponCommand(
                CouponCode.of(request.code()),
                request.maxUses(),
                CountryCode.of(request.country())));

        URI location = UriComponentsBuilder.fromPath("/api/v1/coupons/{code}")
                .buildAndExpand(coupon.code().value())
                .toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @GetMapping("/{code}")
    CouponResponse get(@PathVariable String code) {
        return CouponResponse.from(couponService.get(CouponCode.of(code)));
    }
}