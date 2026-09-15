package com.ozerler.marble.controller.erp;

import com.ozerler.marble.service.ReportService;
import com.ozerler.marble.util.ExportFilenames;
import com.ozerler.marble.util.HttpDownloads;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'FINANCE', 'FACTORY_MANAGER')")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String index(Model model) {
        // Collect preview data for all 6 reports
        Map<String, ReportService.ReportData> reportsData = new LinkedHashMap<>();
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            reportsData.put(type.name(), reportService.getReportData(type));
        }

        model.addAttribute("reports", reportsData);
        model.addAttribute("reportTypes", ReportService.ReportType.values());
        model.addAttribute("currentDate", LocalDate.now().toString());
        model.addAttribute("currentSection", "reports");
        return "erp/reports/index";
    }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable("type") String typeStr,
            @RequestParam(value = "format", defaultValue = "excel") String format) throws IOException {

        ReportService.ReportType type;
        try {
            type = ReportService.ReportType.valueOf(typeStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvBytes = reportService.generateCsvReport(type);
            return HttpDownloads.attachment(
                    csvBytes,
                    ExportFilenames.build(type.getExportFilenameStem(), "csv"),
                    MediaType.parseMediaType("text/csv; charset=UTF-8"));
        }

        byte[] excelBytes = reportService.generateExcelReport(type);
        return HttpDownloads.attachment(
                excelBytes,
                ExportFilenames.build(type.getExportFilenameStem(), "xlsx"),
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
