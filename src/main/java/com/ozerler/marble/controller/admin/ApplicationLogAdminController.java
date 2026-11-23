package com.ozerler.marble.controller.admin;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.ApplicationLogKpiDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.service.ApplicationLogService;
import com.ozerler.marble.web.ApplicationLogGrid;
import com.ozerler.marble.web.GridRequests;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/admin/settings/logs")
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE')")
@RequiredArgsConstructor
public class ApplicationLogAdminController extends AbstractController {

    private static final List<String> AVAILABLE_LEVELS = List.of("ERROR", "WARN", "INFO", "DEBUG");

    private final ApplicationLogService applicationLogService;

    @Getter
    @Setter
    public static class LogPageQuery {
        private Integer page;
        private Integer size;
        private String search;
        private String sort;
        private String dir;
        private String sortField;
        private String sortDir;
        private String level;
        private String levels;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
    }

    public String logsPage(Model model) {
        return logsPage(new LogPageQuery(), model);
    }

    @GetMapping
    public String logsPage(@ModelAttribute LogPageQuery query, Model model) {
        int pageNo = GridRequests.page(query.getPage());
        int pageSize = GridRequests.size(query.getSize());
        String sortKey = GridRequests.sort(query.getSort(), query.getSortField());
        String sortDirection = GridRequests.direction(query.getDir(), query.getSortDir());
        List<String> selectedLevels = new ArrayList<>();
        if (query.getLevels() != null && !query.getLevels().isBlank()) {
            selectedLevels.add(query.getLevels());
        }
        ApplicationLogKpiDto kpis = applicationLogService.getKpiMetrics();
        model.addAttribute("kpis", kpis);
        model.addAttribute("availableLevels", AVAILABLE_LEVELS);
        model.addAttribute("currentSection", "settings");
        model.addAttribute(
                "grid",
                ApplicationLogGrid.view(
                        applicationLogService.getLogs(
                                pageNo,
                                pageSize,
                                query.getSearch(),
                                sortKey,
                                sortDirection,
                                combineLevelFilters(selectedLevels, query.getLevel()),
                                query.getStartDate(),
                                query.getEndDate()),
                        pageNo,
                        pageSize,
                        sortKey,
                        sortDirection));
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
            @RequestParam(value = "startDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {

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

    @GetMapping("/api/kpis")
    @ResponseBody
    public ApplicationLogKpiDto getKpiMetrics() {
        return applicationLogService.getKpiMetrics();
    }

    @PostMapping("/cleanup")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam(value = "retentionDays", defaultValue = "30") int retentionDays) {
        int deleted = applicationLogService.cleanupOldLogs(retentionDays);
        String message;
        if (retentionDays == 0) {
            message = "Tüm log kayıtları başarıyla temizlendi (" + deleted + " adet).";
        } else {
            message = deleted + " adet eski log kaydı başarıyla temizlendi.";
        }
        ApplicationLogKpiDto updatedKpis = applicationLogService.getKpiMetrics();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "deletedCount", deleted,
                "message", message,
                "kpis", updatedKpis
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
