

package com.asyncsourav.quicktix.config;



import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;



@Configuration
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {


    /**
     * Allowed origins or origin patterns
     * Configurable in application.yml or via CORS_ALLOWED_ORIGINS environment variable.
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000",
            "http://localhost:8080"
    ));

    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));

    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Cookie", "Set-Cookie"
    ));

    private List<String> exposedHeaders = new ArrayList<>(List.of(
            "Authorization", "Set-Cookie"
    ));

    private boolean allowCredentials = true;
}
