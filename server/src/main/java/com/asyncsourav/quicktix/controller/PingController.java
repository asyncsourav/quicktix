
package com.asyncsourav.quicktix.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;




@Tag(name = "Health", description = "Endpoints for service health checks")
@RestController
@RequestMapping("/api")
public class PingController {


    @Operation(summary = "Ping service health", description = "Simple health check endpoint returning server status and timestamp.")
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("service", "QuickTix API");
        response.put("message", "Backend is running smoothly!");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}

