package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IpAddressTest {

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "185.15.59.224", "2001:4860:4860::8888"})
    void publicAddressesAreGeolocatable(String raw) {
        assertThat(IpAddress.of(raw).isPublic()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.1.2.3", "172.16.0.1", "192.168.1.1", "169.254.1.1", "100.64.0.1",
            "0.0.0.0", "224.0.0.1", "::1", "fd00::1", "fe80::1"})
    void nonPublicAddressesAreNotGeolocatable(String raw) {
        assertThat(IpAddress.of(raw).isPublic()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "example.com", "dead.beef", "256.1.1.1", "1.2.3", "1.2.3.4/../x", "::g", "1.2.3.4 ; DROP"})
    void malformedValuesAreRejectedWithoutDnsLookup(String raw) {
        assertThatThrownBy(() -> IpAddress.of(raw)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void ipv4MappedIpv6IsNormalisedToIpv4() {
        assertThat(IpAddress.of("::ffff:8.8.8.8").value()).isEqualTo("8.8.8.8");
    }
}
