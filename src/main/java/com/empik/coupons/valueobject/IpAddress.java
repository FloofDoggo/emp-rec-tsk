package com.empik.coupons.valueobject;

import com.empik.coupons.exception.InvalidInputException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public record IpAddress(String value) {
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9A-Fa-f:.]{2,45}$");

    public IpAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("IP address must not be blank");
        }
        value = parse(value.strip()).getHostAddress();
    }

    public static IpAddress of(String raw) {
        return new IpAddress(raw);
    }

    public boolean isPublic() {
        InetAddress address = parse(value);
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            return !(first == 100 && second >= 64 && second <= 127); // 100.64.0.0/10 (CGNAT)
        }
        return (bytes[0] & 0xFE) != 0xFC; // fc00::/7 (unique local addresses)
    }

    private static InetAddress parse(String candidate) {
        boolean looksLikeIpv6 = candidate.indexOf(':') >= 0;
        if (looksLikeIpv6 ? !IPV6_CHARS.matcher(candidate).matches() : !isValidIpv4Shape(candidate)) {
            throw new InvalidInputException("Invalid IP address");
        }
        try {
            return InetAddress.getByName(candidate);
        } catch (UnknownHostException e) {
            throw new InvalidInputException("Invalid IP address");
        }
    }

    private static boolean isValidIpv4Shape(String candidate) {
        var matcher = IPV4.matcher(candidate);
        if (!matcher.matches()) {
            return false;
        }
        for (int group = 1; group <= 4; group++) {
            if (Integer.parseInt(matcher.group(group)) > 255) {
                return false;
            }
        }
        return true;
    }
}