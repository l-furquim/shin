package com.shin.upload.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Upload Service API")
                        .version("v1")
                        .description("Upload service API - handles video uploads, chunking, and processing")
                        .contact(new Contact()
                                .name("Shin API Support")
                                .url("https://github.com/yourorg/shin")));
    }
}
