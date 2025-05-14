package com.handson.tinyurl.config;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class CassandraConfig {

    @Bean("cassandraSession")
    public CqlSession getCassandraSession() {
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress("3.125.50.55", 9042))
                .withKeyspace("tiny_keyspace")
                .withLocalDatacenter("datacenter1")
                .build();
    }
}