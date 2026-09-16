package com.ozerler.marble.controller.admin;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping({"/admin/dashboard/systemhealth", "/admin/dashboard/systemhealth/"})
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class SystemHealthController extends AbstractController {

    private final SystemHealthService systemHealthService;

    @GetMapping({"", "/"})
    public String systemHealthPage(Model model) {
        Map<String, Object> healthData = systemHealthService.collectHealthMetrics();
        model.addAllAttributes(healthData);
        model.addAttribute("currentSection", "systemhealth");
        model.addAttribute("rawJson", systemHealthService.formatHealthJson(healthData));

        return "admin/system-health";
    }

    @GetMapping("/api")
    public @ResponseBody BackEndResponse systemHealthApi() {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Collecting system health metrics API data");
            Map<String, Object> metrics = systemHealthService.collectHealthMetrics();

            HttpHeaders responseHeaders = new HttpHeaders();
            ResponseEntity<Map<String, Object>> resp = new ResponseEntity<>(metrics, responseHeaders, HttpStatus.OK);

            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("System health metrics collected successfully");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in systemHealthApi", e);
            ber = buildFatalResponse(ber, serviceStatus, status, "systemHealthApi", Constants.ERR_FATAL);
        }

        return ber;
    }
}
