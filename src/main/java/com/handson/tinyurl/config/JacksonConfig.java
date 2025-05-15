package com.handson.tinyurl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    public JacksonConfig(ObjectMapper objectMapper) {
        objectMapper.registerModule(new JodaModule());
    }
}