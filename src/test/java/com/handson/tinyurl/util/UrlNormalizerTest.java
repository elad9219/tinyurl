
package com.handson.tinyurl.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlNormalizerTest {

    @Test
    void addsHttpsWhenProtocolIsMissing() {
        assertEquals(
                "https://example.com",
                UrlNormalizer.normalize("example.com")
        );
    }

    @Test
    void preservesWwwWhenProvided() {
        assertEquals(
                "https://www.example.com",
                UrlNormalizer.normalize("www.example.com")
        );
    }

    @Test
    void doesNotAddWwwAutomatically() {
        assertEquals(
                "https://example.com",
                UrlNormalizer.normalize("https://example.com")
        );
    }

    @Test
    void preservesHttpUrls() {
        assertEquals(
                "http://example.com/page",
                UrlNormalizer.normalize("http://example.com/page")
        );
    }

    @Test
    void preservesQueryParametersAndFragments() {
        assertEquals(
                "https://example.com/page?item=123&sort=asc#reviews",
                UrlNormalizer.normalize(
                        "example.com/page?item=123&sort=asc#reviews"
                )
        );
    }

    @Test
    void preservesPortsAndPaths() {
        assertEquals(
                "https://example.com:8443/path",
                UrlNormalizer.normalize(
                        "https://example.com:8443/path"
                )
        );
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(
                "https://example.com",
                UrlNormalizer.normalize("  example.com  ")
        );
    }

    @Test
    void rejectsEmptyInput() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize("  ")
        );
    }

    @Test
    void rejectsUnsupportedProtocols() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize("ftp://example.com")
        );
    }

    @Test
    void rejectsInvalidUrls() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize("https://example .com")
        );
    }

    @Test
    void rejectsUrlsWithCredentials() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize(
                        "https://user:password@example.com/path"
                )
        );
    }

    @Test
    void rejectsSingleWordAsDomain() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize(
                        "flkjdnakjfnsakjdfn"
                )
        );
    }

    @Test
    void rejectsSingleWordWithHttps() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize(
                        "https://d3d3ds3e"
                )
        );
    }

    @Test
    void rejectsInvalidTopLevelDomain() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize(
                        "https://csafdw.dwd2wd"
                )
        );
    }

    @Test
    void acceptsExplicitLocalhostUrl() {
        assertEquals(
                "http://localhost:8080/path",
                UrlNormalizer.normalize(
                        "http://localhost:8080/path"
                )
        );
    }

    @Test
    void rejectsBareLocalhost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize("localhost")
        );
    }

    @Test
    void acceptsValidIpv4Address() {
        assertEquals(
                "http://127.0.0.1:8080/path",
                UrlNormalizer.normalize(
                        "http://127.0.0.1:8080/path"
                )
        );
    }

    @Test
    void rejectsInvalidIpv4Address() {
        assertThrows(
                IllegalArgumentException.class,
                () -> UrlNormalizer.normalize(
                        "http://999.168.1.1/path"
                )
        );
    }

    @Test
    void acceptsValidIpv6Address() {
        assertEquals(
                "http://[::1]:8080/path",
                UrlNormalizer.normalize(
                        "http://[::1]:8080/path"
                )
        );
    }
}
