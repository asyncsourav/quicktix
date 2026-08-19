package com.asyncsourav.quicktix.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/*
 * =========================================================================================
 * BEGINNER VS STANDARD APPROACH EXPLANATION:
 * 
 * NOOB / BEGINNER WAY:
 * -------------------
 * @Controller
 * public class PingController {
 *     @RequestMapping(value = "/api/ping", method = RequestMethod.GET)
 *     @ResponseBody
 *     public String ping() {
 *         // Beginner returning raw JSON string manually (error-prone!)
 *         return "{\"status\":\"UP\", \"message\":\"Backend is up\"}";
 *     }
 * }
 * 
 * STANDARD / PRODUCTION WAY:
 * --------------------------
 * 1. Use @RestController: Combines @Controller and @ResponseBody automatically so method return values are serialized into JSON.
 * 2. Use @RequestMapping("/api"): Groups base URL path at class level.
 * 3. Use @GetMapping("/ping"): Shortcut annotation for HTTP GET request.
 * 4. Return ResponseEntity<Map<String, String>>: Standard Spring container wrapper to return proper HTTP status code (200 OK) with clean JSON body.
 * =========================================================================================
 */

@RestController
@RequestMapping("/api")
public class PingController {

    /**
     * Health check endpoint to verify backend service status.
     * Accessible at: GET http://localhost:8080/api/ping
     */
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
