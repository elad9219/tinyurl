package com.handson.tinyurl.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    // Inject the MongoDB URI dynamically from application.properties
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        // Matches the target database name
        return "tinydb";
    }

    @Override
    public MongoClient mongoClient() {
        // Create the MongoClient securely using the injected URI
        return MongoClients.create(mongoUri);
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }
}