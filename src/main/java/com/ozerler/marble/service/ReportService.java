package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.repository.*;
import com.ozerler.marble.util.Csvs;
import com.ozerler.marble.util.DateTimes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final BlockRepository blockRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final CutOrderRepository cutOrderRepository;
    private final ProjectRepository projectRepository;
    private final SlabRepository slabRepository;
    private final CostAccountingService costAccountingService;
    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    public enum ReportType {
        QUARRY_BLOCKS("ocak_bloklari"),
        FACTORY_SCRAP("katrak_fire"),
        WORKSHOP_ORDERS("kesim_emirleri"),
        SITE_INSTALLATION("santiye_projeleri"),
        COST_ACCOUNTING("maliyet_raporu"),
        SLABS_INVENTORY("plaka_stogu");

        private final String exportFilenameStem;

        ReportType(String exportFilenameStem) {
            this.exportFilenameStem = exportFilenameStem;
        }

        public String getTitle() {
            return com.ozerler.marble.util.MessageUtils.getMessage("report." + name().toLowerCase() + ".title");
        }

        public String getDescription() {
            return com.ozerler.marble.util.MessageUtils.getMessage("report." + name().toLowerCase() + ".desc");
        }

        public String getExportFilenameStem() {
            return exportFilenameStem;
        }
    }

    public static class ReportData {
        public String key;
        public String title;
        public String description;
        public String[] headers;
        public List<String[]> rows = new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public ReportData getReportData(ReportType type) {
        ReportData data = new ReportData();
        data.key = type.name();
        data.title = type.getTitle();
        data.description = type.getDescription();

        switch (type) {
            case QUARRY_BLOCKS -> {
                data.headers = new String[]{
                        getMessage("report.header.block_code"),
                        getMessage("report.header.quarry"),
                        getMessage("report.header.quality"),
                        getMessage("report.header.volume"),
                        getMessage("report.header.theoretical_tonnage"),
                        getMessage("report.header.actual_tonnage"),
                        getMessage("report.header.deviation"),
                        getMessage("report.header.status"),
                        getMessage("report.header.stone_type")
                };
                List<Block> blocks = blockRepository.findAllWithQuarry();
                for (Block b : blocks) {
                    String varianceStr = "0.00%";
                    if (b.getWeightDeviationPct() != null) {
                        varianceStr = String.format("%.2f%%", b.getWeightDeviationPct().doubleValue());
                    }
                    String theoTon = "0.00";
                    if (b.getTheoreticalWeightKg() != null) {
                        theoTon = b.getTheoreticalWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString();
                    }
                    String actualTon = "0.00";
                    if (b.getActualWeightKg() != null) {
                        actualTon = b.getActualWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString();
                    }
                    String stoneType = b.getStoneType() != null ? b.getStoneType() : getMessage("common.default_stone_type");
                    data.rows.add(new String[]{
                            b.getBlockCode() != null ? b.getBlockCode() : "",
                            b.getQuarry() != null ? b.getQuarry().getName() : "",
                            b.getQualityGrade() != null ? b.getQualityGrade().name() : "",
                            b.getVolumeM3() != null ? b.getVolumeM3().toString() : "0.00",
                            theoTon,
                            actualTon,
                            varianceStr,
                            b.getStatus() != null ? b.getStatus().name() : "",
                            stoneType
                    });
                }
            }
            case FACTORY_SCRAP -> {
                data.headers = new String[]{
                        getMessage("report.header.logged_at"),
                        getMessage("report.header.block_code"),
                        getMessage("report.header.scrap_code"),
                        getMessage("report.header.scrap_reason"),
                        getMessage("report.header.scrap_area"),
                        getMessage("report.header.logged_by")
                };
                List<ScrapLog> scraps = scrapLogRepository.findAllWithBlock();
                for (ScrapLog s : scraps) {
                    data.rows.add(new String[]{
                            DateTimes.formatYearMonthDayHourMinute(s.getLoggedAt(), ""),
                            s.getBlock() != null ? s.getBlock().getBlockCode() : "-",
                            s.getReasonCode() != null ? s.getReasonCode().getCode() : "",
                            s.getReasonCode() != null ? s.getReasonCode().getTitle() : "",
                            s.getScrapAreaM2() != null ? s.getScrapAreaM2().toString() : "0.00",
                            s.getLoggedBy() != null ? s.getLoggedBy() : getMessage("common.system")
                    });
                }
            }
            case WORKSHOP_ORDERS -> {
                data.headers = new String[]{
                        getMessage("report.header.order_no"),
                        getMessage("report.header.project"),
                        getMessage("report.header.machine"),
                        getMessage("report.header.operator"),
                        getMessage("report.header.pieces_count"),
                        getMessage("report.header.status"),
                        getMessage("report.header.logged_at")
                };
                List<CutOrder> orders = cutOrderRepository.findAllWithProject();
                for (CutOrder o : orders) {
                    String siteName = o.getProject() != null ? o.getProject().getName() : "-";
                    String itemsCount = String.valueOf(o.getItems() != null ? o.getItems().size() : 0);
                    data.rows.add(new String[]{
                            o.getCutOrderNo() != null ? o.getCutOrderNo() : "",
                            siteName,
                            o.getMachineName() != null ? o.getMachineName() : "-",
                            o.getOperatorName() != null ? o.getOperatorName() : "-",
                            getMessage("common.unit.pieces", itemsCount),
                            o.getStatus() != null ? o.getStatus() : "",
                            DateTimes.formatYearMonthDayHourMinute(o.getCreatedAt(), "-")
                    });
                }
            }
            case SITE_INSTALLATION -> {
                data.headers = new String[]{
                        getMessage("report.header.project_code"),
                        getMessage("report.header.project_name"),
                        getMessage("report.header.customer_company"),
                        getMessage("report.header.contract_value"),
                        getMessage("report.header.actual_cost"),
                        getMessage("report.header.start_date"),
                        getMessage("report.header.delivery_date"),
                        getMessage("report.header.status")
                };
                List<Project> projects = projectRepository.findAll();
                for (Project p : projects) {
                    data.rows.add(new String[]{
                            p.getProjectCode() != null ? p.getProjectCode() : "",
                            p.getName() != null ? p.getName() : "",
                            p.getCustomerName() != null ? p.getCustomerName() : "",
                            p.getContractValue() != null ? p.getContractValue().toString() + " ₺" : "0.00 ₺",
                            p.getActualCost() != null ? p.getActualCost().toString() + " ₺" : "0.00 ₺",
                            p.getStartDate() != null ? p.getStartDate().toString() : "-",
                            p.getDeliveryDate() != null ? p.getDeliveryDate().toString() : "-",
                            p.getStatus() != null ? p.getStatus().name() : ""
                    });
                }
            }
            case COST_ACCOUNTING -> {
                data.headers = new String[]{
                        getMessage("report.header.cost_center"),
                        getMessage("report.header.code"),
                        getMessage("report.header.description"),
                        getMessage("report.header.monthly_budget"),
                        getMessage("report.header.ref_unit_cost"),
                        getMessage("report.header.suggested_price")
                };
                var centers = costAccountingService.getAllCostCenters();
                for (var cc : centers) {
                    BigDecimal unitCost = new BigDecimal("1365.00");
                    BigDecimal recPrice = unitCost.divide(BigDecimal.valueOf(0.65), 2, RoundingMode.HALF_UP);
                    data.rows.add(new String[]{
                            cc.getName() != null ? cc.getName() : "",
                            cc.getCode() != null ? cc.getCode() : "",
                            cc.getDescription() != null ? cc.getDescription() : "-",
                            cc.getMonthlyBudget() != null ? cc.getMonthlyBudget().toString() + " ₺" : "0.00 ₺",
                            unitCost.toString() + " ₺/m²",
                            recPrice.toString() + " ₺/m²"
                    });
                }
            }
            case SLABS_INVENTORY -> {
                data.headers = new String[]{
                        getMessage("report.header.slab_barcode"),
                        getMessage("report.header.source_block"),
                        getMessage("report.header.dimensions"),
                        getMessage("report.header.thickness"),
                        getMessage("report.header.surface_finish"),
                        getMessage("report.header.quality_grade"),
                        getMessage("report.header.unit_cost"),
                        getMessage("report.header.status")
                };
                List<Slab> slabs = slabRepository.findAllWithBlock();
                for (Slab sl : slabs) {
                    String dims = (sl.getWidthCm() != null ? sl.getWidthCm() : 0) + "x" + (sl.getLengthCm() != null ? sl.getLengthCm() : 0) + " cm";
                    data.rows.add(new String[]{
                            sl.getSlabCode() != null ? sl.getSlabCode() : "",
                            sl.getBlock() != null ? sl.getBlock().getBlockCode() : "-",
                            dims,
                            sl.getThicknessCm() != null ? sl.getThicknessCm() + " cm" : "2 cm",
                            sl.getSurfaceFinish() != null ? sl.getSurfaceFinish().name() : "POLISHED",
                            sl.getQualityGrade() != null ? sl.getQualityGrade().name() : "A",
                            sl.getCostPerM2() != null ? sl.getCostPerM2().toString() + " ₺" : "1365.00 ₺",
                            sl.getStatus() != null ? sl.getStatus().name() : "AVAILABLE"
                    });
                }
            }
        }
        return data;
    }

    @Transactional(readOnly = true)
    public byte[] generateExcelReport(ReportType type) throws IOException {
        ReportData data = getReportData(type);

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(getMessage("report.sheet.name"));
            if (sheet instanceof SXSSFSheet sxSheet) {
                sxSheet.trackAllColumnsForAutoSizing();
            }

            // Title row styling
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(data.title + " - " + getMessage("system.health.app_name"));
            titleCell.setCellStyle(titleStyle);

            // Header styling
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Data styling
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Headers row
            Row headerRow = sheet.createRow(2);
            for (int i = 0; i < data.headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(data.headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 3;
            for (String[] rowData : data.rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int col = 0; col < rowData.length; col++) {
                    Cell c = row.createCell(col);
                    c.setCellValue(rowData[col]);
                    c.setCellStyle(dataStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < data.headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1200, 20000));
            }

            workbook.write(out);
            return out.toByteArray();
        } finally {
            workbook.dispose();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateCsvReport(ReportType type) {
        ReportData data = getReportData(type);
        StringWriter sw = new StringWriter();
        // Add UTF-8 BOM for Microsoft Excel Turkish character compatibility
        sw.write('\ufeff');

        PrintWriter pw = new PrintWriter(sw);

        // Header line
        for (int i = 0; i < data.headers.length; i++) {
            pw.print(Csvs.escapeField(data.headers[i]));
            if (i < data.headers.length - 1) pw.print(";");
        }
        pw.println();

        // Rows
        for (String[] row : data.rows) {
            for (int i = 0; i < row.length; i++) {
                pw.print(Csvs.escapeField(row[i]));
                if (i < row.length - 1) pw.print(";");
            }
            pw.println();
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }
}
