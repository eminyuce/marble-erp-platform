package com.ozerler.marble.controller.admin;

import com.ozerler.marble.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping({"/admin/dashboard/systemhealth", "/admin/dashboard/systemhealth/"})
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
@RequiredArgsConstructor
public class SystemHealthController {

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
    @ResponseBody
    public ResponseEntity<Map<String, Object>> systemHealthApi() {
        return ResponseEntity.ok(systemHealthService.collectHealthMetrics());
    }
}
