package com.ozerler.marble.controller.erp;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ozerler.marble.service.ReportService;
import com.ozerler.marble.util.ExportFilenames;
import com.ozerler.marble.util.HttpDownloads;
import com.ozerler.marble.web.ListQuery;
import com.ozerler.marble.web.ReportGrid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'EXECUTIVE', 'FINANCE', 'FACTORY_MANAGER')")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public String index(@RequestParam(value = "report", required = false) String report,
                        @ModelAttribute ListQuery query,
                        Model model) {
        String selected = knownReport(report) ? report : "QUARRY_BLOCKS";
        return indexWithSelected(model, selected, query);
    }

    @GetMapping("/quarry")
    public String quarryReport(@ModelAttribute ListQuery query, Model model) {
        return indexWithSelected(model, "QUARRY_BLOCKS", query);
    }

    @GetMapping("/factory")
    public String factoryReport(@ModelAttribute ListQuery query, Model model) {
        return indexWithSelected(model, "FACTORY_INVENTORY", query);
    }

    @GetMapping("/invoices")
    public String invoiceReport(@ModelAttribute ListQuery query, Model model) {
        return indexWithSelected(model, "INVOICE_FLOW", query);
    }

    @GetMapping("/collections")
    public String collectionReport(@ModelAttribute ListQuery query, Model model) {
        return indexWithSelected(model, "COLLECTIONS_SUMMARY", query);
    }

    @GetMapping("/work-orders")
    public String workOrderReport(@ModelAttribute ListQuery query, Model model) {
        return indexWithSelected(model, "OPERATION_WORK_ORDERS", query);
    }

    private String indexWithSelected(Model model, String selectedKey, ListQuery query) {
        Map<String, ReportService.ReportData> reportsData = new LinkedHashMap<>();
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            reportsData.put(type.name(), reportService.getReportData(type));
        }
        String key = knownReport(selectedKey) ? selectedKey : "QUARRY_BLOCKS";

        model.addAttribute("reports", reportsData);
        model.addAttribute("reportLinks", reportLinks());
        model.addAttribute("reportTypes", ReportService.ReportType.values());
        model.addAttribute("currentDate", LocalDate.now().toString());
        model.addAttribute("currentSection", "reports");
        model.addAttribute("defaultSelectedReport", key);
        model.addAttribute("selectedReport", reportsData.get(key));
        model.addAttribute("grid", ReportGrid.view(reportsData.get(key), query.pageNumber(), query.pageSize()));
        return "erp/reports/index";
    }

    private static boolean knownReport(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            if (type.name().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> reportLinks() {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("QUARRY_BLOCKS", "/reports/quarry");
        links.put("FACTORY_INVENTORY", "/reports/factory");
        links.put("INVOICE_FLOW", "/reports/invoices");
        links.put("COLLECTIONS_SUMMARY", "/reports/collections");
        links.put("OPERATION_WORK_ORDERS", "/reports/work-orders");
        for (ReportService.ReportType type : ReportService.ReportType.values()) {
            links.putIfAbsent(type.name(), "/reports?report=" + type.name());
        }
        return links;
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
