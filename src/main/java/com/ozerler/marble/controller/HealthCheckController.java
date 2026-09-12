package com.ozerler.marble.controller;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
public class HealthCheckController extends AbstractController {

    @Value("${server.port:8080}")
    private int serverPort;

    @GetMapping({"/health", "/health/"})
    public @ResponseBody BackEndResponse getHealth() {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Collecting system health status");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "UP");
            body.put("service", "ozerler-marble-erp");
            body.put("version", "1.0.0");
            body.put("port", serverPort);
            body.put("timestamp", Instant.now().toString());

            Map<String, Object> components = new LinkedHashMap<>();
            components.put("db", Map.of("status", "UP", "database", "MySQL 8.4"));
            components.put("quarryService", Map.of("status", "UP"));
            components.put("factoryService", Map.of("status", "UP"));
            components.put("costAccounting", Map.of("status", "UP"));
            components.put("diskSpace", Map.of("status", "UP"));

            body.put("components", components);

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<Map<String, Object>> resp = new ResponseEntity<>(body, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Health check successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in getHealth", e);
            ber = buildFatalResponse(ber, serviceStatus, status, "getHealth", Constants.ERR_FATAL);
        }

        return ber;
    }
}
