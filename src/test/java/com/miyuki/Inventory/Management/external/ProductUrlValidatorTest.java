package com.miyuki.Inventory.Management.external;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductUrlValidatorTest {

    private final ProductUrlValidator validator = new ProductUrlValidator("shop.example");

    @Test
    void acceptsHttpsUrlOnExactConfiguredHost() {
        assertEquals("shop.example", validator.validate("https://shop.example/product/1").getHost());
    }

    @Test
    void rejectsUnconfiguredHostsAndSubdomainLookalikes() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://evil.example/product"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://shop.example.evil.test/product"));
    }

    @Test
    void rejectsInsecureAndUnsafeUrls() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://shop.example/product"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://localhost/product"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://127.0.0.1/product"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://user@shop.example/product"));
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https://shop.example:8443/product"));
    }

    @Test
    void emptyAllowlistRejectsAllDomainsWithoutBreakingConstruction() {
        ProductUrlValidator noConfiguredHosts = new ProductUrlValidator("");

        assertThrows(IllegalArgumentException.class,
                () -> noConfiguredHosts.validate("https://shop.example/product"));
    }
}
