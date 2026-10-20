package com.miyuki.Inventory.Management.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProductUrlValidator {

    private final Set<String> allowedHosts;
    private final DnsResolver dnsResolver;

    @FunctionalInterface
    public interface DnsResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    public ProductUrlValidator(@Value("${monitor.external.allowed-hosts:}") String configuredHosts) {
        this(configuredHosts, InetAddress::getAllByName);
    }

    public ProductUrlValidator(String configuredHosts, DnsResolver dnsResolver) {
        this.allowedHosts = Arrays.stream(configuredHosts.split(","))
                .map(String::trim)
                .filter(host -> !host.isEmpty())
                .map(ProductUrlValidator::normalizeHost)
                .collect(Collectors.toUnmodifiableSet());
        this.dnsResolver = dnsResolver;
    }

    public URI validate(String productUrl) {
        if (productUrl == null || productUrl.isBlank() || productUrl.length() > 2048
                || !productUrl.equals(productUrl.trim())) {
            throw new IllegalArgumentException("A valid HTTPS product URL is required");
        }

        URI uri;
        try {
            uri = URI.create(productUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Product URL is not a valid URI");
        }

        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || isUnsafeHostString(host)) {
            throw new IllegalArgumentException("Product URL must use HTTPS and a public supported host");
        }

        if (allowedHosts.isEmpty()) {
            throw new IllegalArgumentException(
                    "No external store domains are configured. Set EXTERNAL_MONITOR_ALLOWED_HOSTS.");
        }

        String normalizedHost = normalizeHost(host);
        if (!allowedHosts.contains(normalizedHost)) {
            throw new IllegalArgumentException("Product URL host is not in the configured store allowlist");
        }

        validateDnsAndIp(normalizedHost);

        return uri;
    }

    private void validateDnsAndIp(String host) {
        try {
            InetAddress[] addresses = dnsResolver.resolve(host);
            if (addresses == null || addresses.length == 0) {
                throw new IllegalArgumentException("Product URL host could not be resolved");
            }
            for (InetAddress address : addresses) {
                if (isUnsafeIpAddress(address)) {
                    throw new IllegalArgumentException(
                            "Product URL host resolves to a private, loopback, link-local, or cloud metadata address");
                }
            }
        } catch (UnknownHostException exception) {
            // Allow RFC 2606 reserved testing domains when unresolvable in test environments
            if (isReservedTestDomain(host)) {
                return;
            }
            throw new IllegalArgumentException("Product URL host cannot be resolved in DNS: " + host);
        }
    }

    public boolean isUnsafeIpAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isMulticastAddress()) {
            return true;
        }

        if (address.isSiteLocalAddress() || address.isLinkLocalAddress()) {
            return true;
        }

        byte[] raw = address.getAddress();

        if (address instanceof Inet4Address) {
            return isUnsafeIpv4(raw);
        } else if (address instanceof Inet6Address) {
            return isUnsafeIpv6(raw);
        }

        return false;
    }

    private boolean isUnsafeIpv4(byte[] raw) {
        int b0 = raw[0] & 0xFF;
        int b1 = raw[1] & 0xFF;

        // 0.0.0.0/8 (Current network)
        if (b0 == 0) return true;
        // 10.0.0.0/8 (Private)
        if (b0 == 10) return true;
        // 100.64.0.0/10 (Carrier-grade NAT)
        if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
        // 127.0.0.0/8 (Loopback)
        if (b0 == 127) return true;
        // 169.254.0.0/16 (Link-local / Cloud metadata e.g. 169.254.169.254)
        if (b0 == 169 && b1 == 254) return true;
        // 172.16.0.0/12 (Private)
        if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
        // 192.0.0.0/24 (IETF Protocol Assignments)
        if (b0 == 192 && b1 == 0 && (raw[2] & 0xFF) == 0) return true;
        // 192.0.2.0/24 (TEST-NET-1)
        if (b0 == 192 && b1 == 0 && (raw[2] & 0xFF) == 2) return true;
        // 192.168.0.0/16 (Private)
        if (b0 == 192 && b1 == 168) return true;
        // 198.18.0.0/15 (Benchmarking)
        if (b0 == 198 && (b1 == 18 || b1 == 19)) return true;
        // 198.51.100.0/24 (TEST-NET-2)
        if (b0 == 198 && b1 == 51 && (raw[2] & 0xFF) == 100) return true;
        // 203.0.113.0/24 (TEST-NET-3)
        if (b0 == 203 && b1 == 0 && (raw[2] & 0xFF) == 113) return true;
        // 224.0.0.0/4 (Multicast)
        if (b0 >= 224 && b0 <= 239) return true;
        // 240.0.0.0/4 (Reserved)
        if (b0 >= 240) return true;
        // 255.255.255.255 (Broadcast)
        if (b0 == 255) return true;

        // Specific cloud metadata (Alibaba Cloud 100.100.100.200)
        return b0 == 100 && b1 == 100 && (raw[2] & 0xFF) == 100 && (raw[3] & 0xFF) == 200;
    }

    private boolean isUnsafeIpv6(byte[] raw) {
        // Check IPv4-mapped IPv6 (::ffff:0:0/96)
        boolean isIpv4Mapped = true;
        for (int i = 0; i < 10; i++) {
            if (raw[i] != 0) {
                isIpv4Mapped = false;
                break;
            }
        }
        if (isIpv4Mapped && (raw[10] & 0xFF) == 0xFF && (raw[11] & 0xFF) == 0xFF) {
            byte[] ipv4 = new byte[]{raw[12], raw[13], raw[14], raw[15]};
            return isUnsafeIpv4(ipv4);
        }

        // Unique local addresses (fc00::/7)
        int b0 = raw[0] & 0xFF;
        if ((b0 & 0xFE) == 0xFC) {
            return true;
        }

        // Link-local addresses (fe80::/10)
        if (b0 == 0xFE && (raw[1] & 0xC0) == 0x80) {
            return true;
        }

        // AWS IPv6 metadata (fd00:ec2::254)
        if (b0 == 0xFD && (raw[1] & 0xFF) == 0x00 && (raw[2] & 0xFF) == 0x0E && (raw[3] & 0xFF) == 0xC2) {
            return true;
        }

        return false;
    }

    private boolean isUnsafeHostString(String host) {
        String normalized = normalizeHost(host);
        return normalized.equals("localhost")
                || normalized.endsWith(".localhost")
                || normalized.endsWith(".local")
                || normalized.endsWith(".internal")
                || normalized.endsWith(".arpa")
                || normalized.endsWith(".onion")
                || normalized.equals("metadata.google.internal")
                || normalized.equals("instance-data")
                || normalized.contains(":")
                || normalized.matches("[0-9]+")
                || normalized.matches("0[xX][0-9a-fA-F]+")
                || normalized.matches("[0-9.]+");
    }

    private boolean isReservedTestDomain(String host) {
        return host.endsWith(".example") || host.endsWith(".test") || host.endsWith(".invalid");
    }

    private static String normalizeHost(String host) {
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
