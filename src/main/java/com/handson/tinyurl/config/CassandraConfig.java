package com.handson.tinyurl.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Configuration
public class CassandraConfig {

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.username}")
    private String username;

    @Value("${spring.cassandra.password}")
    private String password;

    @Value("${astra.db.secure-connect-bundle:}")
    private String bundleName;

    @Value("${astra.db.secure-connect-bundle-base64:}")
    private String bundleBase64;

    @Bean
    public CqlSession getCassandraSession() throws IOException {
        try (InputStream bundleStream = openBundle()) {
            return CqlSession.builder()
                    .withCloudSecureConnectBundle(bundleStream)
                    .withAuthCredentials(username, password)
                    .withKeyspace(keyspace)
                    .build();
        }
    }

    private InputStream openBundle() throws IOException {

        if (bundleBase64 != null && !bundleBase64.isBlank()) {
            try {
                byte[] bundleBytes = Base64.getMimeDecoder()
                        .decode(bundleBase64.trim());

                if (bundleBytes.length == 0) {
                    throw new IllegalStateException(
                            "Astra secure connect bundle is empty."
                    );
                }

                return new ByteArrayInputStream(bundleBytes);

            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "Invalid Base64 Astra secure connect bundle.",
                        e
                );
            }
        }

        if (bundleName != null && !bundleName.isBlank()) {
            ClassPathResource resource =
                    new ClassPathResource(bundleName);

            if (!resource.exists()) {
                throw new IllegalStateException(
                        "Astra secure connect bundle was not found."
                );
            }

            return resource.getInputStream();
        }

        throw new IllegalStateException(
                "Astra secure connect bundle is not configured."
        );
    }
}
