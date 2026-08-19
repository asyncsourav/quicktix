
package com.asyncsourav.quicktix.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;




@RestController
@RequestMapping("/api")
public class PingController {


    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("service", "QuickTix API");
        response.put("message", "Backend is running smoothly!");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {

        Map<String, Object> response = new HashMap<>();

        response.put("status", "UP");
        response.put("Message", "All api are live and working");
        response.put("service", "QuickTix API");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}

