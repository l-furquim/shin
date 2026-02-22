package com.shin.metadata.config;

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
                        .title("Metadata Service API")
                        .version("v1")
                        .description("Metadata management service API - handles videos, playlists, and metadata")
                        .contact(new Contact()
                                .name("Shin API Support")
                                .url("https://github.com/l-furquim/shin")));
    }
}
