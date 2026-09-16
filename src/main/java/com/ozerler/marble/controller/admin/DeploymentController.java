package com.ozerler.marble.controller.admin;

import com.ozerler.marble.dto.DeploymentLogDto;
import com.ozerler.marble.dto.DeploymentStartResult;
import com.ozerler.marble.dto.DeploymentStatusDto;
import com.ozerler.marble.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/admin/deployment")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class DeploymentController {

    private final DeploymentService deploymentService;

    @GetMapping({"", "/"})
    public String page(Model model) {
        model.addAttribute("currentSection", "settings");
        model.addAttribute("deploymentStatus", deploymentService.currentStatus());
        return "admin/deployment";
    }

    @GetMapping("/status")
    @ResponseBody
    public DeploymentStatusDto status() {
        return deploymentService.currentStatus();
    }

    @GetMapping("/log")
    @ResponseBody
    public DeploymentLogDto log(@RequestParam(name = "offset", defaultValue = "0") long offset) {
        return deploymentService.readLog(offset);
    }

    @PostMapping("/start")
    @ResponseBody
    public ResponseEntity<DeploymentStartResult> start() {
        DeploymentStartResult result = deploymentService.startDeploy();
        if (result.isAccepted()) {
            return ResponseEntity.accepted().body(result);
        }
        HttpStatus status = switch (result.getState()) {
            case RUNNING -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(result);
    }
}
