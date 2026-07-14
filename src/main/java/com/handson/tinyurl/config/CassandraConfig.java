package com.handson.tinyurl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;

@Configuration
public class CassandraConfig {

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.data.cassandra.username}")
    private String username;

    @Value("${spring.data.cassandra.password}")
    private String password;

    @Value("${astra.db.secure-connect-bundle}")
    private String bundleName;

    @Bean
    public CqlSession getCassandraSession() throws IOException {
        ClassPathResource resource = new ClassPathResource(bundleName);
        return CqlSession.builder()
                .withCloudSecureConnectBundle(resource.getInputStream())
                .withAuthCredentials(username, password)
                .withKeyspace(keyspace)
                .build();
    }
}