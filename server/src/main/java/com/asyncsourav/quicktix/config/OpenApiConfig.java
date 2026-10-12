

package com.asyncsourav.quicktix.config;



import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;



@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "QuickTix Event Ticketing API",
                version = "v1.0",
                description = "High-concurrency event ticketing platform REST API documentation with optimistic locking concurrency control.",
                contact = @Contact(
                        name = "Sourav Kumar",
                        email = "xsouravkumar357@gmail.com",
                        url = "https://github.com/asyncsourav/quicktix"
                )
        ),
        tags = {
                @Tag(name = "Auth", description = "Endpoints for user registration and JWT authentication"),
                @Tag(name = "Events", description = "Endpoints for browsing, creating, and filtering events"),
                @Tag(name = "Venues", description = "Endpoints for venue setup and management"),
                @Tag(name = "Bookings", description = "Endpoints for seat reservation and booking management with concurrency safety"),
                @Tag(name = "Admin", description = "Endpoints for platform metrics, user management, and booking oversight"),
                @Tag(name = "Health", description = "Endpoints for service health checks")
        },
        security = {
                @SecurityRequirement(name = "BearerAuth")
        }
)
@SecurityScheme(
        name = "BearerAuth",
        description = "JWT Bearer token authentication. Enter your token in the format: <token>",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer sortTagsCustomizer() {
        final List<String> desiredOrder = List.of(
                "Auth",
                "Events",
                "Venues",
                "Bookings",
                "Admin",
                "Health"
        );

        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.getTags().sort(Comparator.comparingInt(tag -> {
                    int index = desiredOrder.indexOf(tag.getName());
                    return index != -1 ? index : Integer.MAX_VALUE;
                }));
            }
        };
    }
}
