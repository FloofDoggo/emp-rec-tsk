package com.empik.coupons.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated
@ConfigurationProperties("coupons.geolocation")
public record GeolocationProperties(
        @NotBlank @DefaultValue("https://ipwho.is") String baseUrl,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("3s") Duration readTimeout) {
}