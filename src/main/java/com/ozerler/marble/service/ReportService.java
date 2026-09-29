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
    private final StockItemRepository stockItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CollectionRecordRepository collectionRecordRepository;
    private final CheckRecordRepository checkRecordRepository;
    private final OperationWorkOrderRepository operationWorkOrderRepository;
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
        FACTORY_INVENTORY("fabrika_stoklari"),
        INVOICE_FLOW("fatura_raporu"),
        COLLECTIONS_SUMMARY("tahsilat_ve_cekler"),
        OPERATION_WORK_ORDERS("is_emirleri"),
        FACTORY_SCRAP("katrak_fire"),
        WORKSHOP_ORDERS("atelye_is_emirleri"),
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

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String[] getHeaders() {
            return headers;
        }

        public List<String[]> getRows() {
            return rows;
        }
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
                        "Blok Kodu",
                        "Ocak",
                        "Taş Türü",
                        "Tahmini Tonaj",
                        "Gerçek Tonaj",
                        "Sapma %",
                        "Hedef",
                        "Müşteri",
                        "Durum"
                };
                List<Block> blocks = blockRepository.findAllWithQuarry();
                for (Block b : blocks) {
                    String varianceStr = "-";
                    if (b.getWeightDeviationPct() != null) {
                        varianceStr = String.format("%.2f%%", b.getWeightDeviationPct().doubleValue());
                    }
                    String theoTon = "0.00 t";
                    if (b.getTheoreticalWeightKg() != null) {
                        theoTon = b.getTheoreticalWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString() + " t";
                    }
                    String actualTon = "-";
                    if (b.getActualWeightKg() != null) {
                        actualTon = b.getActualWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString() + " t";
                    }
                    String stoneType = b.getStoneType() != null ? b.getStoneType() : "-";
                    String dest = b.getTargetDestination() != null ? b.getTargetDestination().getDisplayName() : "-";
                    String cust = b.getCustomer() != null ? b.getCustomer().getCompanyName() : "-";
                    data.rows.add(new String[]{
                            b.getBlockCode() != null ? b.getBlockCode() : "",
                            b.getQuarry() != null ? b.getQuarry().getName() : "-",
                            stoneType,
                            theoTon,
                            actualTon,
                            varianceStr,
                            dest,
                            cust,
                            b.getStatus() != null ? b.getStatus().name() : ""
                    });
                }
            }
            case FACTORY_INVENTORY -> {
                data.headers = new String[]{
                        "Kategori",
                        "Kod / Numara",
                        "Kaynak Blok",
                        "Taş Türü",
                        "Ölçü / Kalınlık",
                        "Miktar",
                        "Birim",
                        "Adet",
                        "Müşteri / Tahsis",
                        "Durum"
                };
                List<Block> factoryBlocks = blockRepository.findAll();
                for (Block b : factoryBlocks) {
                    if (b.getStatus() == com.ozerler.marble.model.enums.BlockStatus.AT_FACTORY || b.getStatus() == com.ozerler.marble.model.enums.BlockStatus.FACTORY_STOCK) {
                        String ton = b.getActualWeightKg() != null
                                ? b.getActualWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString()
                                : (b.getTheoreticalWeightKg() != null ? b.getTheoreticalWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString() : "0.00");
                        String cust = b.getCustomer() != null ? b.getCustomer().getCompanyName() : "Genel Stok";
                        data.rows.add(new String[]{
                                "Blok Stok",
                                b.getBlockCode(),
                                "-",
                                b.getStoneType() != null ? b.getStoneType() : "-",
                                (b.getLengthCm() != null ? b.getLengthCm() : 0) + "x" + (b.getWidthCm() != null ? b.getWidthCm() : 0) + "x" + (b.getHeightCm() != null ? b.getHeightCm() : 0) + " cm",
                                ton,
                                "ton",
                                "1",
                                cust,
                                b.getStatus() != null ? b.getStatus().name() : "-"
                        });
                    }
                }
                List<Slab> slabs = slabRepository.findAll();
                for (Slab sl : slabs) {
                    String cust = sl.getCustomer() != null ? sl.getCustomer().getCompanyName() : "Genel Stok";
                    String dims = (sl.getWidthCm() != null ? sl.getWidthCm() : 0) + "x" + (sl.getLengthCm() != null ? sl.getLengthCm() : 0) + " (" + (sl.getThicknessCm() != null ? sl.getThicknessCm() : 2) + "cm)";
                    String src = sl.getBlock() != null ? sl.getBlock().getBlockCode() : "-";
                    data.rows.add(new String[]{
                            "Plaka Stok",
                            sl.getSlabCode() != null ? sl.getSlabCode() : "-",
                            src,
                            sl.getStoneType() != null ? sl.getStoneType() : "-",
                            dims,
                            sl.getAreaM2() != null ? sl.getAreaM2().toString() : "0.00",
                            "m²",
                            "1",
                            cust,
                            sl.getStatus() != null ? sl.getStatus().getLabel() : "-"
                    });
                }
                List<StockItem> sizedItems = stockItemRepository.findAll();
                for (StockItem si : sizedItems) {
                    String cust = si.getCustomer() != null ? si.getCustomer().getCompanyName() : "Genel Stok";
                    String dims = (si.getWidthCm() != null ? si.getWidthCm() : 0) + "x" + (si.getLengthCm() != null ? si.getLengthCm() : 0) + " (" + (si.getThicknessCm() != null ? si.getThicknessCm() : 2) + "cm)";
                    String src = si.getSourceBlock() != null ? si.getSourceBlock().getBlockCode() : "-";
                    data.rows.add(new String[]{
                            "Ebatlı Ürün",
                            si.getItemCode() != null ? si.getItemCode() : "-",
                            src,
                            si.getStoneType() != null ? si.getStoneType() : "-",
                            dims,
                            si.getQuantity() != null ? si.getQuantity().toString() : "0.00",
                            si.getUnit() != null ? si.getUnit() : "m2",
                            String.valueOf(si.getPieceCount() != null ? si.getPieceCount() : 1),
                            cust,
                            si.getStatus() != null ? si.getStatus() : "-"
                    });
                }
            }
            case INVOICE_FLOW -> {
                data.headers = new String[]{
                        "Fatura No",
                        "Fatura Tarihi",
                        "Fatura Türü",
                        "Departman",
                        "Cari / Müşteri",
                        "Tutar (KDV'siz)",
                        "Durum",
                        "Açıklama"
                };
                List<Invoice> invoices = invoiceRepository.findAll();
                for (Invoice inv : invoices) {
                    String cust = inv.getCustomer() != null ? inv.getCustomer().getCompanyName() : "-";
                    String amt = inv.getTotalAmount() != null ? String.format("%,.2f TL", inv.getTotalAmount().doubleValue()) : "0,00 TL";
                    data.rows.add(new String[]{
                            inv.getInvoiceNo() != null ? inv.getInvoiceNo() : "-",
                            inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "-",
                            inv.getInvoiceType() != null ? inv.getInvoiceType().getDisplayName() : "-",
                            inv.getDepartment() != null ? inv.getDepartment().getDisplayName() : "-",
                            cust,
                            amt,
                            inv.getStatus() != null ? inv.getStatus().getDisplayName() : "-",
                            inv.getNotes() != null ? inv.getNotes() : "-"
                    });
                }
            }
            case COLLECTIONS_SUMMARY -> {
                data.headers = new String[]{
                        "İşlem / Belge No",
                        "Tarih",
                        "Yöntem",
                        "Cari / Müşteri",
                        "Tutar",
                        "Banka",
                        "Vade Tarihi",
                        "Durum / Açıklama"
                };
                List<CollectionRecord> collections = collectionRecordRepository.findAll();
                for (CollectionRecord col : collections) {
                    String cust = col.getCustomer() != null ? col.getCustomer().getCompanyName() : "-";
                    String amt = col.getAmount() != null ? String.format("%,.2f TL", col.getAmount().doubleValue()) : "0,00 TL";
                    String checkNo = col.getCheckRecord() != null && col.getCheckRecord().getCheckNumber() != null ? col.getCheckRecord().getCheckNumber() : (col.getCollectionNo() != null ? col.getCollectionNo() : ("COL-" + col.getId()));
                    String dueDate = col.getCheckRecord() != null && col.getCheckRecord().getDueDate() != null ? col.getCheckRecord().getDueDate().toString() : "-";
                    String statDesc = col.getCheckRecord() != null && col.getCheckRecord().getStatus() != null ? col.getCheckRecord().getStatus().getDisplayName() : (col.getNotes() != null ? col.getNotes() : "-");
                    data.rows.add(new String[]{
                            checkNo,
                            col.getCollectionDate() != null ? col.getCollectionDate().toString() : "-",
                            col.getMethod() != null ? col.getMethod().getDisplayName() : "-",
                            cust,
                            amt,
                            col.getBankName() != null ? col.getBankName() : "-",
                            dueDate,
                            statDesc
                    });
                }
            }
            case OPERATION_WORK_ORDERS -> {
                data.headers = new String[]{
                        "İş Emri No",
                        "Departman",
                        "Müşteri",
                        "Taş Türü & Renk",
                        "Ebat (Kalınlık x En x Boy)",
                        "Yüzey İşlemi",
                        "Kenar İşlemi",
                        "Planlanan Miktar",
                        "Termin Tarihi",
                        "Durum"
                };
                List<OperationWorkOrder> workOrders = operationWorkOrderRepository.findAll();
                for (OperationWorkOrder wo : workOrders) {
                    String cust = wo.getCustomer() != null ? wo.getCustomer().getCompanyName() : "-";
                    String stone = (wo.getStoneType() != null ? wo.getStoneType() : "-") + (wo.getStoneColorQuality() != null ? " / " + wo.getStoneColorQuality() : "");
                    String dims = (wo.getStoneThicknessCm() != null ? wo.getStoneThicknessCm() + "cm " : "") +
                            (wo.getWidthCm() != null ? wo.getWidthCm() : 0) + "x" + (wo.getLengthCm() != null ? wo.getLengthCm() : 0) + " cm";
                    String qty = (wo.getPlannedQuantity() != null ? wo.getPlannedQuantity().toString() : "0") + " " + (wo.getUnit() != null ? wo.getUnit() : "m2");
                    data.rows.add(new String[]{
                            wo.getWorkOrderNo() != null ? wo.getWorkOrderNo() : "-",
                            wo.getDepartment() != null ? wo.getDepartment().getDisplayName() : "-",
                            cust,
                            stone,
                            dims,
                            wo.getSurfaceOperation() != null ? wo.getSurfaceOperation() : "-",
                            wo.getEdgeOperation() != null ? wo.getEdgeOperation() : "-",
                            qty,
                            wo.getTargetDate() != null ? wo.getTargetDate().toString() : "-",
                            wo.getStatus() != null ? wo.getStatus().getDisplayName() : "-"
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
                            sl.getSurfaceFinish() != null ? sl.getSurfaceFinish().getLabel() : "",
                            sl.getQualityGrade() != null ? sl.getQualityGrade().getLabel() : "",
                            sl.getCostPerM2() != null ? sl.getCostPerM2().toString() + " ₺" : "1365.00 ₺",
                            sl.getStatus() != null ? sl.getStatus().getLabel() : ""
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
