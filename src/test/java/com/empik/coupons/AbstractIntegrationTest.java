package com.empik.coupons;

import com.empik.coupons.client.CountryResolver;
import com.empik.coupons.valueobject.CountryCode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Shared base: one Spring context and one PostgreSQL container for all integration tests. Only the external
 * geolocation provider is replaced; everything else (controllers, services, SQL, Flyway) is real.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected CountryResolver countryResolver;

    @Autowired
    protected JdbcClient jdbc;

    @BeforeEach
    void cleanDatabaseAndStubGeolocation() {
        jdbc.sql("DELETE FROM coupon_redemptions").update();
        jdbc.sql("DELETE FROM coupons").update();
        when(countryResolver.resolve(any())).thenReturn(Optional.of(CountryCode.of("PL")));
    }
}