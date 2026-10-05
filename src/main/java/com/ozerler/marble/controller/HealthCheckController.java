package com.ozerler.marble.controller;

import com.ozerler.marble.dto.HealthResponse;
import com.ozerler.marble.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final SystemHealthService systemHealthService;

    @GetMapping({"/health", "/health/"})
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse health = systemHealthService.buildHealthResponse();
        HttpStatus httpStatus = "UP".equals(health.getStatus()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }
}
