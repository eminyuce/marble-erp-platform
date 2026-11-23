package com.ozerler.marble.service;

import java.util.Objects;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.ScrapLog;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.repository.BlockRepository;
import com.ozerler.marble.repository.CheckRecordRepository;
import com.ozerler.marble.repository.CollectionRecordRepository;
import com.ozerler.marble.repository.CutOrderRepository;
import com.ozerler.marble.repository.InvoiceRepository;
import com.ozerler.marble.repository.OperationWorkOrderRepository;
import com.ozerler.marble.repository.ProjectRepository;
import com.ozerler.marble.repository.ScrapLogRepository;
import com.ozerler.marble.repository.SlabRepository;
import com.ozerler.marble.repository.StockItemRepository;
import com.ozerler.marble.util.Csvs;
import com.ozerler.marble.util.DateTimes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private static final int COLUMN_PADDING = 1200;
    private static final int MAX_COLUMN_WIDTH = 20000;
    private static final double TARGET_COST_RATIO = 0.65;
    private static final int TITLE_FONT_POINTS = 14;

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
                // best effort: fall back to the default below
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
        private String key;
        private String title;
        private String description;
        private String[] headers;
        private List<String[]> rows = new ArrayList<>();

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
                    String stoneType;
                    if (b.getStoneType() != null) {
                        stoneType = b.getStoneType();
                    } else {
                        stoneType = "-";
                    }
                    String dest;
                    if (b.getTargetDestination() != null) {
                        dest = b.getTargetDestination().getDisplayName();
                    } else {
                        dest = "-";
                    }
                    String cust;
                    if (b.getCustomer() != null) {
                        cust = b.getCustomer().getCompanyName();
                    } else {
                        cust = "-";
                    }
                    String name;
                    if (b.getQuarry() != null) {
                        name = b.getQuarry().getName();
                    } else {
                        name = "-";
                    }
                    String value;
                    if (b.getStatus() != null) {
                        value = b.getStatus().name();
                    } else {
                        value = "";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(b.getBlockCode(), ""),
                            name,
                            stoneType,
                            theoTon,
                            actualTon,
                            varianceStr,
                            dest,
                            cust,
                            value
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
                List<Block> factoryBlocks = BoundedReads.all(blockRepository);
                for (Block b : factoryBlocks) {
                    if (b.getStatus() == com.ozerler.marble.model.enums.BlockStatus.AT_FACTORY || b.getStatus() == com.ozerler.marble.model.enums.BlockStatus.FACTORY_STOCK) {
                        String ton;
                        if (b.getActualWeightKg() != null) {
                            ton = b.getActualWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString();
                        } else {
                            if (b.getTheoreticalWeightKg() != null) {
                                ton = b.getTheoreticalWeightKg().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP).toString();
                            } else {
                                ton = "0.00";
                            }
                        }
                        String cust;
                        if (b.getCustomer() != null) {
                            cust = b.getCustomer().getCompanyName();
                        } else {
                            cust = "Genel Stok";
                        }
                        int lengthCm;
                        if (b.getLengthCm() != null) {
                            lengthCm = b.getLengthCm();
                        } else {
                            lengthCm = 0;
                        }
                        int widthCm;
                        if (b.getWidthCm() != null) {
                            widthCm = b.getWidthCm();
                        } else {
                            widthCm = 0;
                        }
                        int heightCm;
                        if (b.getHeightCm() != null) {
                            heightCm = b.getHeightCm();
                        } else {
                            heightCm = 0;
                        }
                        String valueValue;
                        if (b.getStatus() != null) {
                            valueValue = b.getStatus().name();
                        } else {
                            valueValue = "-";
                        }
                        data.rows.add(new String[]{
                                "Blok Stok",
                                b.getBlockCode(),
                                "-",
                                Objects.requireNonNullElse(b.getStoneType(), "-"),
                                lengthCm + "x" + widthCm + "x" + heightCm + " cm",
                                ton,
                                "ton",
                                "1",
                                cust,
                                valueValue
                        });
                    }
                }
                List<Slab> slabs = BoundedReads.all(slabRepository);
                for (Slab sl : slabs) {
                    String cust;
                    if (sl.getCustomer() != null) {
                        cust = sl.getCustomer().getCompanyName();
                    } else {
                        cust = "Genel Stok";
                    }
                    String dims = Objects.requireNonNullElse(sl.getWidthCm(), 0) + "x" + Objects.requireNonNullElse(sl.getLengthCm(), 0) + " (" + Objects.requireNonNullElse(sl.getThicknessCm(), 2) + "cm)";
                    String src;
                    if (sl.getBlock() != null) {
                        src = sl.getBlock().getBlockCode();
                    } else {
                        src = "-";
                    }
                    String valueValue2;
                    if (sl.getAreaM2() != null) {
                        valueValue2 = sl.getAreaM2().toString();
                    } else {
                        valueValue2 = "0.00";
                    }
                    String label;
                    if (sl.getStatus() != null) {
                        label = sl.getStatus().getLabel();
                    } else {
                        label = "-";
                    }
                    data.rows.add(new String[]{
                            "Plaka Stok",
                            Objects.requireNonNullElse(sl.getSlabCode(), "-"),
                            src,
                            Objects.requireNonNullElse(sl.getStoneType(), "-"),
                            dims,
                            valueValue2,
                            "m²",
                            "1",
                            cust,
                            label
                    });
                }
                List<StockItem> sizedItems = BoundedReads.all(stockItemRepository);
                for (StockItem si : sizedItems) {
                    String cust;
                    if (si.getCustomer() != null) {
                        cust = si.getCustomer().getCompanyName();
                    } else {
                        cust = "Genel Stok";
                    }
                    String dims = Objects.requireNonNullElse(si.getWidthCm(), 0) + "x" + Objects.requireNonNullElse(si.getLengthCm(), 0) + " (" + Objects.requireNonNullElse(si.getThicknessCm(), 2) + "cm)";
                    String src;
                    if (si.getSourceBlock() != null) {
                        src = si.getSourceBlock().getBlockCode();
                    } else {
                        src = "-";
                    }
                    String valueValue3;
                    if (si.getQuantity() != null) {
                        valueValue3 = si.getQuantity().toString();
                    } else {
                        valueValue3 = "0.00";
                    }
                    int pieceCount;
                    if (si.getPieceCount() != null) {
                        pieceCount = si.getPieceCount();
                    } else {
                        pieceCount = 1;
                    }
                    data.rows.add(new String[]{
                            "Ebatlı Ürün",
                            Objects.requireNonNullElse(si.getItemCode(), "-"),
                            src,
                            Objects.requireNonNullElse(si.getStoneType(), "-"),
                            dims,
                            valueValue3,
                            Objects.requireNonNullElse(si.getUnit(), "m2"),
                            String.valueOf(pieceCount),
                            cust,
                            Objects.requireNonNullElse(si.getStatus(), "-")
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
                List<Invoice> invoices = BoundedReads.all(invoiceRepository);
                for (Invoice inv : invoices) {
                    String cust;
                    if (inv.getCustomer() != null) {
                        cust = inv.getCustomer().getCompanyName();
                    } else {
                        cust = "-";
                    }
                    String amt;
                    if (inv.getTotalAmount() != null) {
                        amt = String.format("%,.2f TL", inv.getTotalAmount().doubleValue());
                    } else {
                        amt = "0,00 TL";
                    }
                    String valueValue4;
                    if (inv.getInvoiceDate() != null) {
                        valueValue4 = inv.getInvoiceDate().toString();
                    } else {
                        valueValue4 = "-";
                    }
                    String displayName;
                    if (inv.getInvoiceType() != null) {
                        displayName = inv.getInvoiceType().getDisplayName();
                    } else {
                        displayName = "-";
                    }
                    String displayNameValue;
                    if (inv.getDepartment() != null) {
                        displayNameValue = inv.getDepartment().getDisplayName();
                    } else {
                        displayNameValue = "-";
                    }
                    String displayNameValue2;
                    if (inv.getStatus() != null) {
                        displayNameValue2 = inv.getStatus().getDisplayName();
                    } else {
                        displayNameValue2 = "-";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(inv.getInvoiceNo(), "-"),
                            valueValue4,
                            displayName,
                            displayNameValue,
                            cust,
                            amt,
                            displayNameValue2,
                            Objects.requireNonNullElse(inv.getNotes(), "-")
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
                List<CollectionRecord> collections = BoundedReads.all(collectionRecordRepository);
                for (CollectionRecord col : collections) {
                    String cust;
                    if (col.getCustomer() != null) {
                        cust = col.getCustomer().getCompanyName();
                    } else {
                        cust = "-";
                    }
                    String amt;
                    if (col.getAmount() != null) {
                        amt = String.format("%,.2f TL", col.getAmount().doubleValue());
                    } else {
                        amt = "0,00 TL";
                    }
                    String checkNo;
                    if (col.getCheckRecord() != null && col.getCheckRecord().getCheckNumber() != null) {
                        checkNo = col.getCheckRecord().getCheckNumber();
                    } else {
                        if (col.getCollectionNo() != null) {
                            checkNo = col.getCollectionNo();
                        } else {
                            checkNo = "COL-" + col.getId();
                        }
                    }
                    String dueDate;
                    if (col.getCheckRecord() != null && col.getCheckRecord().getDueDate() != null) {
                        dueDate = col.getCheckRecord().getDueDate().toString();
                    } else {
                        dueDate = "-";
                    }
                    String statDesc;
                    if (col.getCheckRecord() != null && col.getCheckRecord().getStatus() != null) {
                        statDesc = col.getCheckRecord().getStatus().getDisplayName();
                    } else {
                        if (col.getNotes() != null) {
                            statDesc = col.getNotes();
                        } else {
                            statDesc = "-";
                        }
                    }
                    String valueValue5;
                    if (col.getCollectionDate() != null) {
                        valueValue5 = col.getCollectionDate().toString();
                    } else {
                        valueValue5 = "-";
                    }
                    String displayNameValue3;
                    if (col.getMethod() != null) {
                        displayNameValue3 = col.getMethod().getDisplayName();
                    } else {
                        displayNameValue3 = "-";
                    }
                    data.rows.add(new String[]{
                            checkNo,
                            valueValue5,
                            displayNameValue3,
                            cust,
                            amt,
                            Objects.requireNonNullElse(col.getBankName(), "-"),
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
                List<OperationWorkOrder> workOrders = BoundedReads.all(operationWorkOrderRepository);
                for (OperationWorkOrder wo : workOrders) {
                    String cust;
                    if (wo.getCustomer() != null) {
                        cust = wo.getCustomer().getCompanyName();
                    } else {
                        cust = "-";
                    }
                    String valueValue6;
                    if (wo.getStoneColorQuality() != null) {
                        valueValue6 = " / " + wo.getStoneColorQuality();
                    } else {
                        valueValue6 = "";
                    }
                    String stone = Objects.requireNonNullElse(wo.getStoneType(), "-") + valueValue6;
                    String valueValue7;
                    if (wo.getStoneThicknessCm() != null) {
                        valueValue7 = wo.getStoneThicknessCm() + "cm ";
                    } else {
                        valueValue7 = "";
                    }
                    String dims = valueValue7
                            + Objects.requireNonNullElse(wo.getWidthCm(), 0) + "x" + Objects.requireNonNullElse(wo.getLengthCm(), 0) + " cm";
                    String valueValue8;
                    if (wo.getPlannedQuantity() != null) {
                        valueValue8 = wo.getPlannedQuantity().toString();
                    } else {
                        valueValue8 = "0";
                    }
                    String qty = valueValue8 + " " + Objects.requireNonNullElse(wo.getUnit(), "m2");
                    String displayNameValue4;
                    if (wo.getDepartment() != null) {
                        displayNameValue4 = wo.getDepartment().getDisplayName();
                    } else {
                        displayNameValue4 = "-";
                    }
                    String valueValue9;
                    if (wo.getTargetDate() != null) {
                        valueValue9 = wo.getTargetDate().toString();
                    } else {
                        valueValue9 = "-";
                    }
                    String displayNameValue5;
                    if (wo.getStatus() != null) {
                        displayNameValue5 = wo.getStatus().getDisplayName();
                    } else {
                        displayNameValue5 = "-";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(wo.getWorkOrderNo(), "-"),
                            displayNameValue4,
                            cust,
                            stone,
                            dims,
                            Objects.requireNonNullElse(wo.getSurfaceOperation(), "-"),
                            Objects.requireNonNullElse(wo.getEdgeOperation(), "-"),
                            qty,
                            valueValue9,
                            displayNameValue5
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
                    String blockCode;
                    if (s.getBlock() != null) {
                        blockCode = s.getBlock().getBlockCode();
                    } else {
                        blockCode = "-";
                    }
                    String code;
                    if (s.getReasonCode() != null) {
                        code = s.getReasonCode().getCode();
                    } else {
                        code = "";
                    }
                    String title;
                    if (s.getReasonCode() != null) {
                        title = s.getReasonCode().getTitle();
                    } else {
                        title = "";
                    }
                    String valueValue10;
                    if (s.getScrapAreaM2() != null) {
                        valueValue10 = s.getScrapAreaM2().toString();
                    } else {
                        valueValue10 = "0.00";
                    }
                    String loggedBy;
                    if (s.getLoggedBy() != null) {
                        loggedBy = s.getLoggedBy();
                    } else {
                        loggedBy = getMessage("common.system");
                    }
                    data.rows.add(new String[]{
                            DateTimes.formatYearMonthDayHourMinute(s.getLoggedAt(), ""),
                            blockCode,
                            code,
                            title,
                            valueValue10,
                            loggedBy
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
                    String siteName;
                    if (o.getProject() != null) {
                        siteName = o.getProject().getName();
                    } else {
                        siteName = "-";
                    }
                    int valueValue11;
                    if (o.getItems() != null) {
                        valueValue11 = o.getItems().size();
                    } else {
                        valueValue11 = 0;
                    }
                    String itemsCount = String.valueOf(valueValue11);
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(o.getCutOrderNo(), ""),
                            siteName,
                            Objects.requireNonNullElse(o.getMachineName(), "-"),
                            Objects.requireNonNullElse(o.getOperatorName(), "-"),
                            getMessage("common.unit.pieces", itemsCount),
                            Objects.requireNonNullElse(o.getStatus(), ""),
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
                List<Project> projects = BoundedReads.all(projectRepository);
                for (Project p : projects) {
                    String valueValue12;
                    if (p.getContractValue() != null) {
                        valueValue12 = p.getContractValue().toString() + " ₺";
                    } else {
                        valueValue12 = "0.00 ₺";
                    }
                    String valueValue13;
                    if (p.getActualCost() != null) {
                        valueValue13 = p.getActualCost().toString() + " ₺";
                    } else {
                        valueValue13 = "0.00 ₺";
                    }
                    String valueValue14;
                    if (p.getStartDate() != null) {
                        valueValue14 = p.getStartDate().toString();
                    } else {
                        valueValue14 = "-";
                    }
                    String valueValue15;
                    if (p.getDeliveryDate() != null) {
                        valueValue15 = p.getDeliveryDate().toString();
                    } else {
                        valueValue15 = "-";
                    }
                    String valueValue16;
                    if (p.getStatus() != null) {
                        valueValue16 = p.getStatus().name();
                    } else {
                        valueValue16 = "";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(p.getProjectCode(), ""),
                            Objects.requireNonNullElse(p.getName(), ""),
                            Objects.requireNonNullElse(p.getCustomerName(), ""),
                            valueValue12,
                            valueValue13,
                            valueValue14,
                            valueValue15,
                            valueValue16
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
                    BigDecimal recPrice = unitCost.divide(BigDecimal.valueOf(TARGET_COST_RATIO), 2, RoundingMode.HALF_UP);
                    String valueValue17;
                    if (cc.getMonthlyBudget() != null) {
                        valueValue17 = cc.getMonthlyBudget().toString() + " ₺";
                    } else {
                        valueValue17 = "0.00 ₺";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(cc.getName(), ""),
                            Objects.requireNonNullElse(cc.getCode(), ""),
                            Objects.requireNonNullElse(cc.getDescription(), "-"),
                            valueValue17,
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
                    String dims = Objects.requireNonNullElse(sl.getWidthCm(), 0) + "x" + Objects.requireNonNullElse(sl.getLengthCm(), 0) + " cm";
                    String blockCodeValue;
                    if (sl.getBlock() != null) {
                        blockCodeValue = sl.getBlock().getBlockCode();
                    } else {
                        blockCodeValue = "-";
                    }
                    String valueValue18;
                    if (sl.getThicknessCm() != null) {
                        valueValue18 = sl.getThicknessCm() + " cm";
                    } else {
                        valueValue18 = "2 cm";
                    }
                    String labelValue;
                    if (sl.getSurfaceFinish() != null) {
                        labelValue = sl.getSurfaceFinish().getLabel();
                    } else {
                        labelValue = "";
                    }
                    String labelValue2;
                    if (sl.getQualityGrade() != null) {
                        labelValue2 = sl.getQualityGrade().getLabel();
                    } else {
                        labelValue2 = "";
                    }
                    String valueValue19;
                    if (sl.getCostPerM2() != null) {
                        valueValue19 = sl.getCostPerM2().toString() + " ₺";
                    } else {
                        valueValue19 = "1365.00 ₺";
                    }
                    String labelValue3;
                    if (sl.getStatus() != null) {
                        labelValue3 = sl.getStatus().getLabel();
                    } else {
                        labelValue3 = "";
                    }
                    data.rows.add(new String[]{
                            Objects.requireNonNullElse(sl.getSlabCode(), ""),
                            blockCodeValue,
                            dims,
                            valueValue18,
                            labelValue,
                            labelValue2,
                            valueValue19,
                            labelValue3
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
            titleFont.setFontHeightInPoints((short) TITLE_FONT_POINTS);
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
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + COLUMN_PADDING, MAX_COLUMN_WIDTH));
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
            if (i < data.headers.length - 1) {
                pw.print(";");
            }
        }
        pw.println();

        // Rows
        for (String[] row : data.rows) {
            for (int i = 0; i < row.length; i++) {
                pw.print(Csvs.escapeField(row[i]));
                if (i < row.length - 1) {
                    pw.print(";");
                }
            }
            pw.println();
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }
}
