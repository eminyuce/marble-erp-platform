package com.ozerler.marble.service;

import com.ozerler.marble.model.*;
import com.ozerler.marble.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final BlockRepository blockRepository;
    private final ScrapLogRepository scrapLogRepository;
    private final CutOrderRepository cutOrderRepository;
    private final ProjectRepository projectRepository;
    private final SlabRepository slabRepository;
    private final CostAccountingService costAccountingService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public enum ReportType {
        QUARRY_BLOCKS("Ocak Blok Üretim ve Satış Raporu"),
        FACTORY_SCRAP("Fabrika Katrak ve Fire Raporu"),
        WORKSHOP_ORDERS("Atölye Ebatlama ve Kesim Raporu"),
        SITE_INSTALLATION("Şantiye Proje ve İlerleme Raporu"),
        COST_ACCOUNTING("Maliyet Muhasebesi ve Fiyatlandırma Raporu"),
        SLABS_INVENTORY("Plaka Stok ve Kalite Dağılım Raporu");

        private final String title;
        ReportType(String title) { this.title = title; }
        public String getTitle() { return title; }
    }

    public static class ReportData {
        public String title;
        public String[] headers;
        public List<String[]> rows = new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public ReportData getReportData(ReportType type) {
        ReportData data = new ReportData();
        data.title = type.getTitle();

        switch (type) {
            case QUARRY_BLOCKS -> {
                data.headers = new String[]{"Blok Kodu", "Ocak", "Kalite", "Hacim (m³)", "Teorik Tonaj", "Kantar Tonajı", "Sapma %", "Durum", "Taş Türü"};
                List<Block> blocks = blockRepository.findAll();
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
                    String stoneType = b.getStoneType() != null ? b.getStoneType() : "Klasik Bej";
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
                data.headers = new String[]{"Kayıt Tarihi", "Blok Kodu", "Fire Kodu", "Fire Sebebi", "Fire Alanı (m²)", "Kayıt Yapan"};
                List<ScrapLog> scraps = scrapLogRepository.findAll();
                for (ScrapLog s : scraps) {
                    data.rows.add(new String[]{
                            s.getLoggedAt() != null ? s.getLoggedAt().format(DATE_FMT) : "",
                            s.getBlock() != null ? s.getBlock().getBlockCode() : "-",
                            s.getReasonCode() != null ? s.getReasonCode().getCode() : "",
                            s.getReasonCode() != null ? s.getReasonCode().getTitle() : "",
                            s.getScrapAreaM2() != null ? s.getScrapAreaM2().toString() : "0.00",
                            s.getLoggedBy() != null ? s.getLoggedBy() : "Sistem"
                    });
                }
            }
            case WORKSHOP_ORDERS -> {
                data.headers = new String[]{"İş Emri No", "Proje", "Makine", "Operatör", "Parça Sayısı", "Durum", "Kayıt Tarihi"};
                List<CutOrder> orders = cutOrderRepository.findAll();
                for (CutOrder o : orders) {
                    String siteName = o.getProject() != null ? o.getProject().getName() : "-";
                    String itemsCount = String.valueOf(o.getItems() != null ? o.getItems().size() : 0);
                    data.rows.add(new String[]{
                            o.getCutOrderNo() != null ? o.getCutOrderNo() : "",
                            siteName,
                            o.getMachineName() != null ? o.getMachineName() : "-",
                            o.getOperatorName() != null ? o.getOperatorName() : "-",
                            itemsCount + " adet",
                            o.getStatus() != null ? o.getStatus() : "",
                            o.getCreatedAt() != null ? o.getCreatedAt().format(DATE_FMT) : "-"
                    });
                }
            }
            case SITE_INSTALLATION -> {
                data.headers = new String[]{"Proje Kodu", "Proje Adı", "Müşteri / Şirket", "Sözleşme Tutarı", "Gerçekleşen Maliyet", "Başlangıç", "Teslim", "Durum"};
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
                data.headers = new String[]{"Maliyet Merkezi", "Kod", "Açıklama", "Aylık Bütçe (TL)", "Referans Birim Maliyet (TL/m²)", "Önerilen Satış Fiyatı (%35 Kar)"};
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
                data.headers = new String[]{"Plaka Barkodu", "Kaynak Blok", "Ebat (GxY)", "Kalınlık", "Yüzey İşlem", "Kalite Sınıfı", "Birim Maliyet (TL/m²)", "Durum"};
                List<Slab> slabs = slabRepository.findAll();
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

    public byte[] generateExcelReport(ReportType type) throws IOException {
        ReportData data = getReportData(type);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Rapor");

            // Title row styling
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(data.title + " - Özerler Mermer ERP");
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
        }
    }

    public byte[] generateCsvReport(ReportType type) {
        ReportData data = getReportData(type);
        StringWriter sw = new StringWriter();
        // Add UTF-8 BOM for Microsoft Excel Turkish character compatibility
        sw.write('\ufeff');

        PrintWriter pw = new PrintWriter(sw);

        // Header line
        for (int i = 0; i < data.headers.length; i++) {
            pw.print(escapeCsv(data.headers[i]));
            if (i < data.headers.length - 1) pw.print(";");
        }
        pw.println();

        // Rows
        for (String[] row : data.rows) {
            for (int i = 0; i < row.length; i++) {
                pw.print(escapeCsv(row[i]));
                if (i < row.length - 1) pw.print(";");
            }
            pw.println();
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        String s = val.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }
}
