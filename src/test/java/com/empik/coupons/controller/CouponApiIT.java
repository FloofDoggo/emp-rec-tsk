package com.empik.coupons.controller;

import com.empik.coupons.AbstractIntegrationTest;
import com.empik.coupons.exception.GeolocationUnavailableException;
import com.empik.coupons.valueobject.CountryCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through HTTP -> service -> SQL -> PostgreSQL, with only the geolocation provider mocked. */
class CouponApiIT extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private ResultActions createCoupon(String code, int maxUses, String country) throws Exception {
        return mvc.perform(post("/api/v1/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","maxUses":%d,"country":"%s"}
                        """.formatted(code, maxUses, country)));
    }

    private ResultActions redeem(String code, String userId) throws Exception {
        return mvc.perform(post("/api/v1/redemptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"%s","userId":"%s"}
                        """.formatted(code, userId)));
    }

    // ---- creating coupons ----

    @Test
    void createsCouponAndReturnsItWithNormalisedCode() throws Exception {
        createCoupon("wiosna", 3, "pl")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/coupons/WIOSNA"))
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.maxUses").value(3))
                .andExpect(jsonPath("$.currentUses").value(0))
                .andExpect(jsonPath("$.country").value("PL"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mvc.perform(get("/api/v1/coupons/wiosna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WIOSNA"));
    }

    @Test
    void codeUniquenessIsCaseInsensitive() throws Exception {
        createCoupon("WIOSNA", 3, "PL").andExpect(status().isCreated());

        createCoupon("wiosna", 10, "DE")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_ALREADY_EXISTS"));
    }

    @Test
    void rejectsInvalidCreateRequests() throws Exception {
        createCoupon("ab", 3, "PL").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        createCoupon("OKCODE", 0, "PL").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxUses").exists());
        createCoupon("OKCODE", 3, "ZZ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(post("/api/v1/coupons").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    // ---- redeeming ----

    @Test
    void redeemsCouponAndIncrementsCounter() throws Exception {
        createCoupon("wiosna", 2, "PL").andExpect(status().isCreated());

        redeem("WiOsNa", "user-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WIOSNA"))
                .andExpect(jsonPath("$.userId").value("user-1"));

        mvc.perform(get("/api/v1/coupons/WIOSNA")).andExpect(jsonPath("$.currentUses").value(1));
    }

    @Test
    void unknownCouponReturns404() throws Exception {
        redeem("NOPE", "user-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COUPON_NOT_FOUND"));
    }

    @Test
    void exhaustedCouponReturnsConflictAndLeavesNoTrace() throws Exception {
        createCoupon("ONCE", 1, "PL").andExpect(status().isCreated());
        redeem("ONCE", "user-1").andExpect(status().isCreated());

        redeem("ONCE", "user-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_EXHAUSTED"));

        assertThat(jdbc.sql("SELECT count(*) FROM coupon_redemptions").query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void sameUserCannotRedeemTwice() throws Exception {
        createCoupon("WIOSNA", 5, "PL").andExpect(status().isCreated());
        redeem("WIOSNA", "user-1").andExpect(status().isCreated());

        redeem("wiosna", "user-1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COUPON_ALREADY_REDEEMED"));

        mvc.perform(get("/api/v1/coupons/WIOSNA")).andExpect(jsonPath("$.currentUses").value(1));
    }

    @Test
    void callerFromOtherCountryIsForbidden() throws Exception {
        createCoupon("WIOSNA", 5, "PL").andExpect(status().isCreated());
        when(countryResolver.resolve(any())).thenReturn(Optional.of(CountryCode.of("DE")));

        redeem("WIOSNA", "user-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COUNTRY_NOT_ALLOWED"));

        mvc.perform(get("/api/v1/coupons/WIOSNA")).andExpect(jsonPath("$.currentUses").value(0));
    }

    @Test
    void geolocationOutageReturns503AndConsumesNothing() throws Exception {
        createCoupon("WIOSNA", 5, "PL").andExpect(status().isCreated());
        when(countryResolver.resolve(any())).thenThrow(new GeolocationUnavailableException("down"));

        redeem("WIOSNA", "user-1")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GEOLOCATION_UNAVAILABLE"));

        mvc.perform(get("/api/v1/coupons/WIOSNA")).andExpect(jsonPath("$.currentUses").value(0));
    }

    @Test
    void rejectsInvalidRedeemRequests() throws Exception {
        mvc.perform(post("/api/v1/redemptions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"WIOSNA\",\"userId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.userId").exists());
    }
}