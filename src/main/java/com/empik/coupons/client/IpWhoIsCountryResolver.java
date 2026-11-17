package com.empik.coupons.client;

import com.empik.coupons.exception.GeolocationUnavailableException;
import com.empik.coupons.exception.InvalidInputException;
import com.empik.coupons.valueobject.CountryCode;
import com.empik.coupons.valueobject.IpAddress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.util.Optional;

@Component
public class IpWhoIsCountryResolver implements CountryResolver {
    private static final Logger log = LoggerFactory.getLogger(IpWhoIsCountryResolver.class);
    private final RestClient restClient;

    public IpWhoIsCountryResolver(RestClient geolocationRestClient) {
        this.restClient = geolocationRestClient;
    }

    @Override
    public Optional<CountryCode> resolve(IpAddress address) {
        if (!address.isPublic()) {
            return Optional.empty();
        }
        try {
            IpWhoIsResponse response = restClient.get()
                    .uri("/{ip}", address.value())
                    .retrieve()
                    .body(IpWhoIsResponse.class);

            if (response == null || !response.success() || response.countryCode() == null) {
                log.warn("Geolocation provider could not resolve {}: {}", address,
                        response == null ? "empty response" : response.message());
                throw new GeolocationUnavailableException("Unable to determine the country of the caller");
            }
            return Optional.of(CountryCode.of(response.countryCode()));
        } catch (RestClientException | InvalidInputException e) {
            log.warn("Geolocation call failed for {}: {}", address, e.toString());
            throw new GeolocationUnavailableException("Unable to determine the country of the caller");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IpWhoIsResponse(
            boolean success,
            @JsonProperty("country_code") String countryCode,
            String message) {
    }
}