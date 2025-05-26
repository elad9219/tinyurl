package com.handson.tinyurl.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Override
    protected String getDatabaseName() {
        return "admin"; // מתאים ל-uri ב-application.properties
    }

    @Override
    public MongoClient mongoClient() {
        String mongoUri = "mongodb://root:bootcamp@node128.codingbc.com:27000/admin?retryWrites=true&w=majority&connectTimeoutMS=10000";
        return MongoClients.create(mongoUri);
    }

    @Override
    public boolean autoIndexCreation() {
        return true;
    }
}