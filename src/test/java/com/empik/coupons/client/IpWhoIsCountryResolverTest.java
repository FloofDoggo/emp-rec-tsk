package com.empik.coupons.client;

import com.empik.coupons.exception.GeolocationUnavailableException;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.IpAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IpWhoIsCountryResolverTest {

    private MockRestServiceServer server;
    private IpWhoIsCountryResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://geo.test");
        server = MockRestServiceServer.bindTo(builder).build();
        resolver = new IpWhoIsCountryResolver(builder.build());
    }

    @Test
    void mapsCountryCodeFromProviderResponseIgnoringUnknownFields() {
        server.expect(requestTo("https://geo.test/8.8.8.8")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"ip":"8.8.8.8","success":true,"country":"United States","country_code":"US","flag":{"emoji":"x"}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(resolver.resolve(IpAddress.of("8.8.8.8"))).contains(CountryCode.of("US"));
        server.verify();
    }

    @Test
    void privateAddressIsNotSentToProvider() {
        assertThat(resolver.resolve(IpAddress.of("192.168.0.10"))).isEmpty();
        server.verify(); // no request was expected, none was made
    }

    @Test
    void providerReportingFailureIsAnOutageNotAForeignCountry() {
        server.expect(requestTo("https://geo.test/8.8.8.8"))
                .andRespond(withSuccess("""
                        {"success":false,"message":"limit reached"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> resolver.resolve(IpAddress.of("8.8.8.8")))
                .isInstanceOf(GeolocationUnavailableException.class);
    }

    @Test
    void serverErrorIsAnOutage() {
        server.expect(requestTo("https://geo.test/8.8.8.8")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> resolver.resolve(IpAddress.of("8.8.8.8")))
                .isInstanceOf(GeolocationUnavailableException.class);
    }
}
