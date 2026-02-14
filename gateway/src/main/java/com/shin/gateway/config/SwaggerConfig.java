package com.shin.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {


    @Bean
    public List<GroupedOpenApi> apis(RouteDefinitionLocator locator) {
        List<GroupedOpenApi> groups = new ArrayList<>();
        
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();
        
        if (definitions != null) {
            definitions.stream()
                    .filter(routeDefinition -> {

                        String routeId = routeDefinition.getId();
                        return routeId != null && 
                               !routeId.equals("swagger-ui") &&
                               routeDefinition.getUri() != null &&
                               routeDefinition.getUri().toString().startsWith("lb://");
                    })
                    .forEach(routeDefinition -> {
                        String serviceName = extractServiceName(routeDefinition.getUri().toString());
                        if (serviceName != null) {
                            List<String> paths = extractPaths(routeDefinition);
                            
                            if (!paths.isEmpty()) {
                                groups.add(GroupedOpenApi.builder()
                                        .group(serviceName)
                                        .pathsToMatch(paths.toArray(new String[0]))
                                        .build());
                            }
                        }
                    });
        }
        
        return groups;
    }

    private String extractServiceName(String uri) {
        if (uri.startsWith("lb://")) {
            return uri.substring(5);
        }
        return null;
    }

    private List<String> extractPaths(RouteDefinition routeDefinition) {
        List<String> paths = new ArrayList<>();
        
        routeDefinition.getPredicates().forEach(predicateDefinition -> {
            if ("Path".equals(predicateDefinition.getName())) {
                predicateDefinition.getArgs().values().forEach(value -> {
                    if (value != null) {
                        paths.add((String) value);
                    }
                });
            }
        });
        
        return paths;
    }
}
