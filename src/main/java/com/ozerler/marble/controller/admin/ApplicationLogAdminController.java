package com.ozerler.marble.controller.admin;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.ApplicationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/settings/logs")
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
@RequiredArgsConstructor
public class ApplicationLogAdminController extends AbstractController {

    private static final List<String> AVAILABLE_LEVELS = List.of("ERROR", "WARN", "INFO", "DEBUG");

    private final ApplicationLogService applicationLogService;

    @GetMapping
    public String logsPage(Model model) {
        ApplicationLogKpiDto kpis = applicationLogService.getKpiMetrics();
        model.addAttribute("kpis", kpis);
        model.addAttribute("availableLevels", AVAILABLE_LEVELS);
        model.addAttribute("currentSection", "settings");
        return "admin/settings/logs";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<ApplicationLogDto> getLogsData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "levels", required = false) List<String> levels,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<String> combinedLevels = combineLevelFilters(levels, level);

        return applicationLogService.getLogs(
                page,
                size,
                search,
                sortField,
                sortDir,
                combinedLevels,
                startDate,
                endDate
        );
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<ApplicationLogDto> getLogDetail(@PathVariable("id") Long id) {
        ApplicationLogDto dto = applicationLogService.getLogById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam(value = "retentionDays", defaultValue = "30") int retentionDays) {
        int deleted = applicationLogService.cleanupOldLogs(retentionDays);
        String message = retentionDays == 0
                ? "Tüm log kayıtları başarıyla temizlendi (" + deleted + " adet)."
                : deleted + " adet eski log kaydı başarıyla temizlendi.";
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deleted,
                "message", message
        ));
    }

    private List<String> combineLevelFilters(List<String> levels, String singleLevel) {
        List<String> result = new ArrayList<>();
        if (levels != null) {
            for (String item : levels) {
                if (item != null && !item.isBlank()) {
                    if (item.contains(",")) {
                        result.addAll(Arrays.asList(item.split(",")));
                    } else {
                        result.add(item.trim());
                    }
                }
            }
        }
        if (singleLevel != null && !singleLevel.isBlank()) {
            if (singleLevel.contains(",")) {
                result.addAll(Arrays.asList(singleLevel.split(",")));
            } else {
                result.add(singleLevel.trim());
            }
        }
        return result.stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
    }
}
