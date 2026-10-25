
package com.handson.tinyurl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("TinyURL API")
                                .description(
                                        "URL shortening and click analytics API"
                                )
                                .version("1.0.0")
                );
    }
}
