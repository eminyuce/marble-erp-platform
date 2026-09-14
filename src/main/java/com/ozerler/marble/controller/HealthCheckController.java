package com.ozerler.marble.controller;

import com.ozerler.marble.dto.HealthResponse;
import com.ozerler.marble.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HealthCheckController {

    private final SystemHealthService systemHealthService;

    @GetMapping({"/health", "/health/"})
    public ResponseEntity<HealthResponse> getHealth() {
        log.info("Collecting system health status");
        HealthResponse health = systemHealthService.buildHealthResponse();
        HttpStatus httpStatus = "UP".equals(health.getStatus()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(health);
    }
}
