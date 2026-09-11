package com.ozerler.marble.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping({"/health", "/health/"})
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "ozerler-marble-erp");
        body.put("version", "1.0.0");
        body.put("port", 81);
        body.put("timestamp", Instant.now().toString());

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("db", Map.of("status", "UP", "database", "MySQL 8.4"));
        components.put("quarryService", Map.of("status", "UP"));
        components.put("factoryService", Map.of("status", "UP"));
        components.put("costAccounting", Map.of("status", "UP"));
        components.put("diskSpace", Map.of("status", "UP"));

        body.put("components", components);
        return ResponseEntity.ok(body);
    }
}
