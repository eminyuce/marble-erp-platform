package com.ozerler.marble.service;

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
            map.put("invoiceDate", inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : "");
            map.put("invoiceType", inv.getInvoiceType() != null ? inv.getInvoiceType().name() : "");
            map.put("invoiceTypeLabel", inv.getInvoiceType() != null ? inv.getInvoiceType().getDisplayName() : "Satış");
            String party = inv.getTargetDepartment() != null
                    ? "Dahili: " + inv.getTargetDepartment().getDisplayName()
                    : inv.getCustomer() != null ? inv.getCustomer().getCompanyName()
                    : inv.getSupplier() != null ? inv.getSupplier().getCompanyName() : inv.getPartyName();
            map.put("partyName", party != null ? party : "—");
            map.put("department", inv.getDepartment() != null ? inv.getDepartment().name() : "");
            map.put("departmentLabel", inv.getDepartment() != null ? inv.getDepartment().getDisplayName() : "Genel");
            map.put("targetDepartment", inv.getTargetDepartment() != null ? inv.getTargetDepartment().name() : "");
            map.put("targetDepartmentLabel", inv.getTargetDepartment() != null ? inv.getTargetDepartment().getDisplayName() : "");
            map.put("isInternalTransfer", inv.isInternalTransfer());
            map.put("projectName", inv.getProject() != null ? inv.getProject().getName() : "");
            map.put("subtotalAmount", inv.getSubtotalAmount() != null ? inv.getSubtotalAmount() : BigDecimal.ZERO);
            map.put("taxRate", inv.getTaxRate() != null ? inv.getTaxRate() : BigDecimal.ZERO);
            map.put("taxAmount", inv.getTaxAmount() != null ? inv.getTaxAmount() : BigDecimal.ZERO);
            map.put("totalAmount", inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO);
            map.put("status", inv.getStatus() != null ? inv.getStatus().name() : "");
            map.put("statusLabel", inv.getStatus() != null ? inv.getStatus().getDisplayName() : "Ödenmedi");
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
        String s = search != null ? search.trim().toLowerCase() : "";

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
                    .limit(500)
                    .toList();
            for (Block b : blocks) {
                Map<String, Object> m = new HashMap<>();
                m.put("category", "BLOCK");
                m.put("categoryLabel", "Blok");
                m.put("id", b.getId());
                m.put("code", b.getBlockCode());
                m.put("name", "Blok: " + b.getBlockCode() + (b.getStoneType() != null ? " (" + b.getStoneType() + ")" : ""));
                m.put("stoneType", b.getStoneType() != null ? b.getStoneType() : "—");
                m.put("dimensions", (b.getWidthCm() != null ? b.getWidthCm() : 0) + "x" + (b.getLengthCm() != null ? b.getLengthCm() : 0) + "x" + (b.getHeightCm() != null ? b.getHeightCm() : 0) + " cm");
                BigDecimal ton = b.getActualTonnage() != null && b.getActualTonnage().compareTo(BigDecimal.ZERO) > 0
                        ? b.getActualTonnage()
                        : b.getEstimatedTonnage() != null ? b.getEstimatedTonnage() : BigDecimal.ONE;
                m.put("quantity", scaleQuantity(ton));
                m.put("unit", "ton");
                m.put("widthCm", b.getWidthCm());
                boolean isMarked = b.getAssignedCustomer() != null;
                boolean isMarkedForCustomer = isMarked && customerId != null
                        && customerId.equals(b.getAssignedCustomer().getId());
                m.put("isMarked", isMarked);
                m.put("isMarkedForCustomer", isMarkedForCustomer);
                m.put("assignedCustomerName", b.getAssignedCustomer() != null ? b.getAssignedCustomer().getCompanyName() : null);
                m.put("unitPrice", b.getSalePrice() != null ? b.getSalePrice() : b.getUnitMarketValuePerTon() != null ? b.getUnitMarketValuePerTon() : BigDecimal.ZERO);
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
                        m.put("unitPrice", fuelTank.getUnitPrice() != null ? fuelTank.getUnitPrice() : BigDecimal.ZERO);
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
                    String catCode = ci.getQuarryCategory() != null ? ci.getQuarryCategory().name() : "CONSUMABLE";
                    String catLabel = ci.getQuarryCategory() != null ? ci.getQuarryCategory().getDisplayName() : "Sarf Malzeme";

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
                        m.put("name", ci.getDescription() != null ? ci.getDescription() : ci.getItemCode());
                        m.put("stoneType", catLabel);
                        m.put("dimensions", "—");
                        m.put("quantity", scaleQuantity(ci.getQuantity()));
                        m.put("unit", ci.getUnit() != null ? ci.getUnit() : "adet");
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
                        .limit(500)
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
                    m.put("quantity", scaleQuantity(sl.getSurfaceAreaM2() != null ? sl.getSurfaceAreaM2() : BigDecimal.ONE));
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
                        .limit(500)
                        .toList();
                for (StockItem st : stockItems) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("category", "SIZED");
                    m.put("categoryLabel", "Ebatlı Stok");
                    m.put("id", st.getId());
                    m.put("code", st.getProductCode() != null ? st.getProductCode() : st.getItemCode());
                    m.put("name", (st.getProductCode() != null ? st.getProductCode() : st.getItemCode()) + " — " + (st.getStoneType() != null ? st.getStoneType() : "Ebatlı Mermer"));
                    m.put("stoneType", st.getStoneType() != null ? st.getStoneType() : "—");
                    m.put("dimensions", (st.getWidthCm() != null ? st.getWidthCm() : "") + "x" + (st.getLengthCm() != null ? st.getLengthCm() : "") + "x" + (st.getThicknessCm() != null ? st.getThicknessCm() : "") + " cm");
                    m.put("quantity", scaleQuantity(st.getQuantity()));
                    m.put("unit", st.getUnit() != null ? st.getUnit() : "m2");
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
