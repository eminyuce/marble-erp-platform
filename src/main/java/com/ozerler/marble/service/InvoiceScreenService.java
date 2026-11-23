package com.ozerler.marble.service;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.InvoiceStatus;
import com.ozerler.marble.model.enums.InvoiceType;
import com.ozerler.marble.model.enums.QuarryCategory;
import com.ozerler.marble.model.enums.SlabStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceScreenService {

    private static final int MAX_ROWS = 500;

    private final InvoiceService invoiceService;
    private final FormLookupService formLookup;
    private final QuarryInventoryService quarryInventoryService;

    public TabulatorResponse<Map<String, Object>> invoicesData(
            int page,
            int size,
            String search,
            InvoiceType invoiceType,
            BusinessUnit department,
            InvoiceStatus status,
            Long customerId) {

        int pageIndex = Math.max(0, page - 1);
        Page<Invoice> invoicePage = invoiceService.searchInvoices(invoiceType, department, status, customerId, null, null, search, pageIndex, size);

        List<Map<String, Object>> data = invoicePage.getContent().stream().map(inv -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", inv.getId());
            map.put("invoiceNumber", inv.getInvoiceNumber());
            Object value;
            if (inv.getInvoiceDate() != null) {
                value = inv.getInvoiceDate().toString();
            } else {
                value = "";
            }
            map.put("invoiceDate", value);
            Object valueValue;
            if (inv.getInvoiceType() != null) {
                valueValue = inv.getInvoiceType().name();
            } else {
                valueValue = "";
            }
            map.put("invoiceType", valueValue);
            Object displayName;
            if (inv.getInvoiceType() != null) {
                displayName = inv.getInvoiceType().getDisplayName();
            } else {
                displayName = "Satış";
            }
            map.put("invoiceTypeLabel", displayName);
            String party;
            if (inv.getTargetDepartment() != null) {
                party = "Dahili: " + inv.getTargetDepartment().getDisplayName();
            } else {
                if (inv.getCustomer() != null) {
                    party = inv.getCustomer().getCompanyName();
                } else {
                    if (inv.getSupplier() != null) {
                        party = inv.getSupplier().getCompanyName();
                    } else {
                        party = inv.getPartyName();
                    }
                }
            }
            map.put("partyName", Objects.requireNonNullElse(party, "—"));
            Object valueValue2;
            if (inv.getDepartment() != null) {
                valueValue2 = inv.getDepartment().name();
            } else {
                valueValue2 = "";
            }
            map.put("department", valueValue2);
            Object displayNameValue;
            if (inv.getDepartment() != null) {
                displayNameValue = inv.getDepartment().getDisplayName();
            } else {
                displayNameValue = "Genel";
            }
            map.put("departmentLabel", displayNameValue);
            Object valueValue3;
            if (inv.getTargetDepartment() != null) {
                valueValue3 = inv.getTargetDepartment().name();
            } else {
                valueValue3 = "";
            }
            map.put("targetDepartment", valueValue3);
            Object displayNameValue2;
            if (inv.getTargetDepartment() != null) {
                displayNameValue2 = inv.getTargetDepartment().getDisplayName();
            } else {
                displayNameValue2 = "";
            }
            map.put("targetDepartmentLabel", displayNameValue2);
            map.put("isInternalTransfer", inv.isInternalTransfer());
            Object name;
            if (inv.getProject() != null) {
                name = inv.getProject().getName();
            } else {
                name = "";
            }
            map.put("projectName", name);
            map.put("subtotalAmount", Objects.requireNonNullElse(inv.getSubtotalAmount(), BigDecimal.ZERO));
            map.put("taxRate", Objects.requireNonNullElse(inv.getTaxRate(), BigDecimal.ZERO));
            map.put("taxAmount", Objects.requireNonNullElse(inv.getTaxAmount(), BigDecimal.ZERO));
            map.put("totalAmount", Objects.requireNonNullElse(inv.getTotalAmount(), BigDecimal.ZERO));
            Object valueValue4;
            if (inv.getStatus() != null) {
                valueValue4 = inv.getStatus().name();
            } else {
                valueValue4 = "";
            }
            map.put("status", valueValue4);
            Object displayNameValue3;
            if (inv.getStatus() != null) {
                displayNameValue3 = inv.getStatus().getDisplayName();
            } else {
                displayNameValue3 = "Ödenmedi";
            }
            map.put("statusLabel", displayNameValue3);
            return map;
        }).toList();

        return TabulatorResponse.of(data, invoicePage.getTotalPages(), invoicePage.getTotalElements());
    }

    public List<Map<String, Object>> availableStock(
            BusinessUnit department,
            String stockType,
            String search,
            Long customerId,
            Boolean includeZero) {

        List<Map<String, Object>> result = new ArrayList<>();
        String s;
        if (search != null) {
            s = search.trim().toLowerCase();
        } else {
            s = "";
        }

        // 1. Bloklar (Ocak & Fabrika filtrelenebilir, müşteri rezervasyonları öncelikli)
        if (stockType == null || stockType.isBlank() || "BLOCK".equalsIgnoreCase(stockType)) {
            List<Block> blocks = formLookup.blocksWithQuarryEntities().stream()
                    .filter(b -> b.getStatus() != BlockStatus.SOLD && b.getStatus() != BlockStatus.IN_TRANSIT)
                    .filter(b -> {
                        if (department == BusinessUnit.QUARRY) {
                            return b.isAtQuarry();
                        } else if (department == BusinessUnit.FACTORY) {
                            return b.isAtFactory();
                        }
                        return true;
                    })
                    .filter(b -> s.isEmpty() || b.getBlockCode().toLowerCase().contains(s) || (b.getStoneType() != null && b.getStoneType().toLowerCase().contains(s)))
                    .sorted((b1, b2) -> {
                        if (customerId != null) {
                            boolean m1 = b1.getAssignedCustomer() != null && b1.getAssignedCustomer().getId().equals(customerId);
                            boolean m2 = b2.getAssignedCustomer() != null && b2.getAssignedCustomer().getId().equals(customerId);
                            if (m1 && !m2) {
                                return -1;
                            }
                            if (!m1 && m2) {
                                return 1;
                            }
                        }
                        return b2.getId().compareTo(b1.getId());
                    })
                    .limit(MAX_ROWS)
                    .toList();
            for (Block b : blocks) {
                Map<String, Object> m = new HashMap<>();
                m.put("category", "BLOCK");
                m.put("categoryLabel", "Blok");
                m.put("id", b.getId());
                m.put("code", b.getBlockCode());
                String value;
                if (b.getStoneType() != null) {
                    value = " (" + b.getStoneType() + ")";
                } else {
                    value = "";
                }
                m.put("name", "Blok: " + b.getBlockCode() + value);
                m.put("stoneType", Objects.requireNonNullElse(b.getStoneType(), "—"));
                int widthCm;
                if (b.getWidthCm() != null) {
                    widthCm = b.getWidthCm();
                } else {
                    widthCm = 0;
                }
                int lengthCm;
                if (b.getLengthCm() != null) {
                    lengthCm = b.getLengthCm();
                } else {
                    lengthCm = 0;
                }
                int heightCm;
                if (b.getHeightCm() != null) {
                    heightCm = b.getHeightCm();
                } else {
                    heightCm = 0;
                }
                m.put("dimensions", widthCm + "x" + lengthCm + "x" + heightCm + " cm");
                BigDecimal ton;
                if (b.getActualTonnage() != null && b.getActualTonnage().compareTo(BigDecimal.ZERO) > 0) {
                    ton = b.getActualTonnage();
                } else {
                    if (b.getEstimatedTonnage() != null) {
                        ton = b.getEstimatedTonnage();
                    } else {
                        ton = BigDecimal.ONE;
                    }
                }
                m.put("quantity", scaleQuantity(ton));
                m.put("unit", "ton");
                m.put("widthCm", b.getWidthCm());
                boolean isMarked = b.getAssignedCustomer() != null;
                boolean isMarkedForCustomer = isMarked && customerId != null
                        && customerId.equals(b.getAssignedCustomer().getId());
                m.put("isMarked", isMarked);
                m.put("isMarkedForCustomer", isMarkedForCustomer);
                Object companyName;
                if (b.getAssignedCustomer() != null) {
                    companyName = b.getAssignedCustomer().getCompanyName();
                } else {
                    companyName = null;
                }
                m.put("assignedCustomerName", companyName);
                Object salePrice;
                if (b.getSalePrice() != null) {
                    salePrice = b.getSalePrice();
                } else {
                    if (b.getUnitMarketValuePerTon() != null) {
                        salePrice = b.getUnitMarketValuePerTon();
                    } else {
                        salePrice = BigDecimal.ZERO;
                    }
                }
                m.put("unitPrice", salePrice);
                result.add(m);
            }
        }

        // 2. Mazot Deposu (Ocak Mazot Stoğu - Madde 3 & 8)
        if ((department == null || department == BusinessUnit.QUARRY)
                && (stockType == null || stockType.isBlank() || "FUEL".equalsIgnoreCase(stockType) || "MAZOT".equalsIgnoreCase(stockType))) {
            StockItem fuelTank = quarryInventoryService.getOrCreateFuelTankStockItem();
            if (fuelTank != null
                    && (Boolean.TRUE.equals(includeZero) || (fuelTank.getQuantity() != null && fuelTank.getQuantity().compareTo(BigDecimal.ZERO) > 0))
                    && (s.isEmpty() || "mazot".contains(s) || "dizel".contains(s) || fuelTank.getItemCode().toLowerCase().contains(s))) {
                Map<String, Object> m = new HashMap<>();
                        m.put("category", "MAZOT");
                        m.put("categoryLabel", "Mazot");
                        m.put("id", fuelTank.getId());
                        m.put("code", fuelTank.getItemCode());
                        m.put("name", "Ocak Mazot Stoğu (Dizel)");
                        m.put("stoneType", "Akaryakıt");
                        m.put("dimensions", "Ana Depo Tankı");
                        m.put("quantity", scaleQuantity(fuelTank.getQuantity()));
                        m.put("unit", "litre");
                        m.put("unitPrice", Objects.requireNonNullElse(fuelTank.getUnitPrice(), BigDecimal.ZERO));
                        m.put("directExpense", false);
                        result.add(m);
            }
        }

        // 3. Sarf Malzeme & Diğer Ocak Stok Kartları (Madde 1 & 2 & 9)
        if (department == null || department == BusinessUnit.QUARRY) {
            List<StockItem> allQuarryCards = quarryInventoryService.getAllQuarryStockCards(null);
            for (StockItem ci : allQuarryCards) {
                if (ci.getItemCode().equals(QuarryInventoryService.FUEL_TANK_ITEM_CODE) || ci.getItemCode().equals("O-MZ-TANK")) {
                    continue;
                }
                if (Boolean.TRUE.equals(includeZero) || (ci.getQuantity() != null && ci.getQuantity().compareTo(BigDecimal.ZERO) > 0)) {
                    String catCode;
                    if (ci.getQuarryCategory() != null) {
                        catCode = ci.getQuarryCategory().name();
                    } else {
                        catCode = "CONSUMABLE";
                    }
                    String catLabel;
                    if (ci.getQuarryCategory() != null) {
                        catLabel = ci.getQuarryCategory().getDisplayName();
                    } else {
                        catLabel = "Sarf Malzeme";
                    }

                    if (stockType != null && !stockType.isBlank()) {
                        boolean match = stockType.equalsIgnoreCase(catCode)
                                || ("CONSUMABLE".equalsIgnoreCase(stockType) && (ci.getQuarryCategory() == null || ci.getQuarryCategory() == QuarryCategory.SARF_MALZEME));
                        if (!match) {
                            continue;
                        }
                    }

                    if (s.isEmpty() || (ci.getDescription() != null && ci.getDescription().toLowerCase().contains(s)) || (ci.getItemCode() != null && ci.getItemCode().toLowerCase().contains(s))) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("category", catCode);
                        m.put("categoryLabel", catLabel);
                        m.put("id", ci.getId());
                        m.put("code", ci.getItemCode());
                        Object description;
                        if (ci.getDescription() != null) {
                            description = ci.getDescription();
                        } else {
                            description = ci.getItemCode();
                        }
                        m.put("name", description);
                        m.put("stoneType", catLabel);
                        m.put("dimensions", "—");
                        m.put("quantity", scaleQuantity(ci.getQuantity()));
                        m.put("unit", Objects.requireNonNullElse(ci.getUnit(), "adet"));
                        m.put("unitPrice", ci.getUnitPrice());
                        m.put("directExpense", Boolean.TRUE.equals(ci.getDirectExpense()));
                        result.add(m);
                    }
                }
            }
        }

        // 4. Plakalar (Fabrika & Atölye)
        if ((department == null || department == BusinessUnit.FACTORY || department == BusinessUnit.WORKSHOP)
                && (stockType == null || stockType.isBlank() || "SLAB".equalsIgnoreCase(stockType))) {
                List<Slab> slabs = formLookup.slabsByStatusEntities(SlabStatus.AVAILABLE).stream()
                        .filter(sl -> s.isEmpty() || sl.getSlabCode().toLowerCase().contains(s))
                        .limit(MAX_ROWS)
                        .toList();
                for (Slab sl : slabs) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", "SLAB");
                    m.put("categoryLabel", "Plaka");
                    m.put("id", sl.getId());
                    m.put("code", sl.getSlabCode());
                    m.put("name", "Plaka: " + sl.getSlabCode());
                    m.put("stoneType", "Plaka");
                    m.put("dimensions", sl.getWidthCm() + "x" + sl.getLengthCm() + "x" + sl.getThicknessCm() + " cm");
                    m.put("quantity", scaleQuantity(Objects.requireNonNullElse(sl.getSurfaceAreaM2(), BigDecimal.ONE)));
                    m.put("unit", "m2");
                    m.put("widthCm", sl.getWidthCm());
                    result.add(m);
                }
        }

        // 5. Ebatlı Ürün Stoğu
        if ((department == null || department == BusinessUnit.FACTORY || department == BusinessUnit.WORKSHOP)
                && (stockType == null || stockType.isBlank() || "SIZED".equalsIgnoreCase(stockType))) {
                List<StockItem> stockItems = formLookup.positiveStockEntities().stream()
                        .filter(st -> {
                            if (department != null && st.getStockLocation() != null) {
                                return department.equals(st.getStockLocation().getBusinessUnit());
                            }
                            return true;
                        })
                        .filter(st -> s.isEmpty() || (st.getProductCode() != null && st.getProductCode().toLowerCase().contains(s)) || (st.getStoneType() != null && st.getStoneType().toLowerCase().contains(s)))
                        .limit(MAX_ROWS)
                        .toList();
                for (StockItem st : stockItems) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", "SIZED");
                    m.put("categoryLabel", "Ebatlı Stok");
                    m.put("id", st.getId());
                    Object productCode;
                    if (st.getProductCode() != null) {
                        productCode = st.getProductCode();
                    } else {
                        productCode = st.getItemCode();
                    }
                    m.put("code", productCode);
                    String productCodeValue;
                    if (st.getProductCode() != null) {
                        productCodeValue = st.getProductCode();
                    } else {
                        productCodeValue = st.getItemCode();
                    }
                    m.put("name", productCodeValue + " — " + Objects.requireNonNullElse(st.getStoneType(), "Ebatlı Mermer"));
                    m.put("stoneType", Objects.requireNonNullElse(st.getStoneType(), "—"));
                    m.put("dimensions", Objects.requireNonNullElse(st.getWidthCm(), "") + "x" + Objects.requireNonNullElse(st.getLengthCm(), "") + "x" + Objects.requireNonNullElse(st.getThicknessCm(), "") + " cm");
                    m.put("quantity", scaleQuantity(st.getQuantity()));
                    m.put("unit", Objects.requireNonNullElse(st.getUnit(), "m2"));
                    m.put("widthCm", st.getWidthCm());
                    result.add(m);
                }
        }

        return result;
    }

    private BigDecimal scaleQuantity(BigDecimal qty) {
        if (qty == null) {
            return BigDecimal.ZERO;
        }
        return qty.setScale(2, RoundingMode.HALF_UP);
    }
}
