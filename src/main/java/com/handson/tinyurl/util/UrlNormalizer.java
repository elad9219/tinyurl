
package com.handson.tinyurl.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class UrlNormalizer {

    private UrlNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a URL");
        }

        String value = input.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        boolean explicitHttp = lower.startsWith("http://")
                || lower.startsWith("https://");

        if (!explicitHttp) {
            if (value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) {
                throw new IllegalArgumentException(
                        "Only HTTP and HTTPS URLs are supported"
                );
            }

            value = "https://" + value;
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException(
                        "Only HTTP and HTTPS URLs are supported"
                );
            }

            if (host == null
                    || host.isEmpty()
                    || !isValidHost(host, explicitHttp)) {
                throw new IllegalArgumentException(
                        "Please enter a complete website address (e.g. example.com)"
                );
            }

            if (uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException(
                        "URLs containing credentials are not supported"
                );
            }

            if (uri.getPort() > 65535) {
                throw new IllegalArgumentException(
                        "The URL contains an invalid port"
                );
            }

            // Preserve the original host, path, query and fragment.
            return uri.toString();

        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Please enter a valid URL", e
            );
        }
    }

    private static boolean isValidHost(
            String host,
            boolean explicitHttp
    ) {
        // Java URI already validates IPv6 address syntax.
        if (host.startsWith("[") && host.endsWith("]")) {
            return true;
        }

        // Allow explicitly specified localhost URLs for development.
        if (host.equalsIgnoreCase("localhost")) {
            return explicitHttp;
        }

        // Allow valid IPv4 addresses.
        if (host.matches("[0-9]+(\\.[0-9]+){3}")) {
            String[] octets = host.split("\\.");

            for (String octet : octets) {
                if (octet.length() > 3
                        || Integer.parseInt(octet) > 255) {
                    return false;
                }
            }

            return true;
        }

        // A valid public-style domain must have at least two labels.
        String dnsName = host.endsWith(".")
                ? host.substring(0, host.length() - 1)
                : host;

        if (dnsName.isEmpty() || dnsName.length() > 253) {
            return false;
        }

        String[] labels = dnsName.split("\\.", -1);

        if (labels.length < 2) {
            return false;
        }

        for (String label : labels) {
            if (!label.matches(
                    "(?i)[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?"
            )) {
                return false;
            }
        }

        String tld = labels[labels.length - 1];

        return tld.matches(
                "(?i)[a-z]{2,63}|xn--[a-z0-9-]{2,59}"
        );
    }
}
