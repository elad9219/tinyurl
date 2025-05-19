package com.handson.tinyurl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.datastax.oss.driver.api.core.CqlSession;
import java.net.InetSocketAddress;

@Configuration
public class CassandraConfig {

    @Bean
    public CqlSession getCassandraSession() {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress("node128.codingbc.com", 9042))
                .withLocalDatacenter("datacenter1")
                .withKeyspace("tiny_keyspace")
                .build();
    }
}