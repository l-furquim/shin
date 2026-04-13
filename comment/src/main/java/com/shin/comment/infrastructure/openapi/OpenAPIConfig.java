package com.shin.comment.infrastructure.openapi;

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
                        .title("Comment Service API")
                        .version("v1")
                        .description("Comment management service API - handles video comments, threads of comments and search")
                        .contact(new Contact()
                                .name("Shin API Support")
                                .url("https://github.com/l-furquim/shin")));
    }
}
