package com.ozerler.marble.web;

import java.util.HashMap;
import java.util.Map;

import com.ozerler.marble.model.ApplicationLog;
import com.ozerler.marble.model.Block;
import com.ozerler.marble.model.CheckRecord;
import com.ozerler.marble.model.CollectionRecord;
import com.ozerler.marble.model.CostCenter;
import com.ozerler.marble.model.CostTransaction;
import com.ozerler.marble.model.Customer;
import com.ozerler.marble.model.CutOrder;
import com.ozerler.marble.model.EmailTemplate;
import com.ozerler.marble.model.Invoice;
import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.ProductionOrder;
import com.ozerler.marble.model.Project;
import com.ozerler.marble.model.PurchaseOrder;
import com.ozerler.marble.model.Quarry;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.Slab;
import com.ozerler.marble.model.StockItem;
import com.ozerler.marble.model.StockLocation;
import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.Supplier;
import com.ozerler.marble.model.User;
import com.ozerler.marble.util.GridPages;

/**
 * Whitelist of header-filter keys per grid table id, mapped onto persistable property paths.
 */
public final class GridFilterCatalog {

    private static final Map<String, Map<String, String>> ALIASES = new HashMap<>();
    private static final Map<String, Class<?>> ENTITIES = new HashMap<>();
    private static final Map<String, Map<String, GridFilterKind>> KINDS = new HashMap<>();

    static {
        Map<String, String> blocks = merge(
                GridPages.BLOCK_SORTS,
                Map.ofEntries(
                        Map.entry("locationName", "currentLocation.name"),
                        Map.entry("colorTone", "colorTone"),
                        Map.entry("quarrySection", "quarrySection"),
                        Map.entry("customerName", "soldCustomer.companyName"),
                        Map.entry("extractionDate", "extractionDate"),
                        Map.entry("arrivalDate", "arrivalDate"),
                        Map.entry("estimatedTonnage", "estimatedTonnage"),
                        Map.entry("approximateTonnage", "estimatedTonnage"),
                        Map.entry("actualTonnage", "actualTonnage"),
                        Map.entry("unitMarketValuePerTon", "unitMarketValuePerTon"),
                        Map.entry("extractionCost", "extractionCost"),
                        Map.entry("salePrice", "salePrice")
                ));
        register("blocks-table", Block.class, blocks);
        register("factory-blocks-table", Block.class, blocks);
        register("quarry-blocks-table", Block.class, blocks);

        Map<String, String> slabs = merge(
                GridPages.SLAB_SORTS,
                Map.of(
                        "customerName", "customer.companyName",
                        "sourceBlockCode", "block.blockCode",
                        "stoneType", "block.stoneType",
                        "thicknessCm", "thicknessCm"));
        register("slabs-table", Slab.class, slabs);
        register("factory-slabs-table", Slab.class, slabs);
        register("workshop-slabs-table", Slab.class, slabs);

        Map<String, String> sized = Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("status", "status"),
                Map.entry("itemCode", "itemCode"),
                Map.entry("productionDate", "productionDate"),
                Map.entry("sourceBlockCode", "sourceBlock.blockCode"),
                Map.entry("description", "description"),
                Map.entry("surfaceFinish", "surfaceFinish"),
                Map.entry("thicknessCm", "thicknessCm"),
                Map.entry("quantity", "quantity"),
                Map.entry("pieceCount", "pieceCount"),
                Map.entry("actualProducedQuantity", "actualProducedQuantity"),
                Map.entry("customerName", "customer.companyName")
        );
        register("factory-sized-table", StockItem.class, sized);
        register("workshop-sized-table", StockItem.class, sized);

        register("production-table", ProductionOrder.class, merge(
                GridPages.PRODUCTION_ORDER_SORTS,
                Map.of("sourceBlockCode", "block.blockCode", "durationHours", "durationHours")));
        register("workshop-table", CutOrder.class, merge(
                GridPages.CUT_ORDER_SORTS,
                Map.of("purpose", "purpose")));
        register("projects-table", Project.class, GridPages.PROJECT_SORTS);
        register("sales-table", SalesOrder.class, GridPages.SALES_ORDER_SORTS);
        register("procurement-table", PurchaseOrder.class, GridPages.PURCHASE_ORDER_SORTS);
        register("users-table", User.class, merge(
                GridPages.USER_SORTS,
                Map.of("firstName", "firstName", "lastName", "lastName")));
        register("suppliers-table", Supplier.class, merge(
                GridPages.SUPPLIER_SORTS,
                Map.of("taxNumber", "taxNumber", "businessUnitLabel", "businessUnit")));
        register("customers-table", Customer.class, merge(
                GridPages.CUSTOMER_SORTS,
                Map.of("businessUnitLabel", "businessUnit")));
        register("machines-table", Machine.class, merge(
                GridPages.MACHINE_SORTS,
                Map.of("notes", "notes")));
        register("stock-locations-table", StockLocation.class, GridPages.STOCK_LOCATION_SORTS);
        register("quarries-table", Quarry.class, GridPages.QUARRY_SORTS);
        register("cost-centers-table", CostCenter.class, merge(
                GridPages.COST_CENTER_SORTS,
                Map.of("description", "description")));
        register("expenses-table", CostTransaction.class, merge(
                GridPages.EXPENSE_SORTS,
                Map.of("description", "description")));

        register("invoices-table", Invoice.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("status", "status"),
                Map.entry("invoiceNumber", "invoiceNo"),
                Map.entry("invoiceDate", "invoiceDate"),
                Map.entry("invoiceTypeLabel", "invoiceType"),
                Map.entry("partyName", "partyName"),
                Map.entry("departmentLabel", "department"),
                Map.entry("subtotalAmount", "subtotalAmount"),
                Map.entry("taxAmount", "taxAmount"),
                Map.entry("totalAmount", "totalAmount")
        ));
        register("collections-table", CollectionRecord.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("method", "method"),
                Map.entry("collectionNo", "collectionNo"),
                Map.entry("collectionDate", "collectionDate"),
                Map.entry("customerName", "customer.companyName"),
                Map.entry("bankName", "bankName"),
                Map.entry("invoiceNo", "invoice.invoiceNo"),
                Map.entry("amount", "amount"),
                Map.entry("notes", "notes")
        ));
        register("checks-table", CheckRecord.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("status", "status"),
                Map.entry("checkNo", "checkNo"),
                Map.entry("dueDate", "dueDate"),
                Map.entry("customerName", "customer.companyName"),
                Map.entry("bankName", "bankName"),
                Map.entry("businessUnitLabel", "businessUnit"),
                Map.entry("amount", "amount")
        ));
        Map<String, String> workOrders = Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("status", "status"),
                Map.entry("orderNo", "orderNo"),
                Map.entry("departmentLabel", "department"),
                Map.entry("customerName", "customer.companyName"),
                Map.entry("stoneType", "stoneType"),
                Map.entry("quantity", "quantity"),
                Map.entry("orderDate", "orderDate"),
                Map.entry("targetDeliveryDate", "targetDeliveryDate"),
                Map.entry("responsiblePerson", "responsiblePerson")
        );
        register("workorders-table", OperationWorkOrder.class, workOrders);
        register("factory-orders-table", OperationWorkOrder.class, workOrders);
        register("workshop-orders-table", OperationWorkOrder.class, workOrders);

        register("logs-table", ApplicationLog.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("level", "level"),
                Map.entry("timestamp", "timestamp"),
                Map.entry("shortLoggerName", "loggerName"),
                Map.entry("message", "message"),
                Map.entry("username", "username"),
                Map.entry("clientIp", "clientIp")
        ));
        register("email-templates-table", EmailTemplate.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("active", "isActive"),
                Map.entry("key", "templateKey"),
                Map.entry("name", "templateName"),
                Map.entry("category", "category"),
                Map.entry("subject", "subject")
        ));
        register("fuel-table", MachineFuelEntry.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("receiptNo", "receiptNo"),
                Map.entry("machineName", "machine.name"),
                Map.entry("entryDate", "entryDate"),
                Map.entry("businessUnitLabel", "machine.businessUnit"),
                Map.entry("litres", "litres"),
                Map.entry("pricePerLitre", "pricePerLitre"),
                Map.entry("totalAmount", "totalAmount"),
                Map.entry("workingHoursOrKm", "workingHoursOrKm")
        ));
        register("quarry-dispatches-table", StockMovement.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("movementDate", "movementDate"),
                Map.entry("tonnage", "tonnage"),
                Map.entry("targetDestination", "targetDestination"),
                Map.entry("customerName", "customer.companyName"),
                Map.entry("invoiceNo", "invoice.invoiceNo"),
                Map.entry("notes", "notes")
        ));
        register("workshop-receipts-table", null, Map.ofEntries(
                Map.entry("receiptNo", "receiptNo"),
                Map.entry("supplierName", "supplierName"),
                Map.entry("sourceLabel", "sourceLabel"),
                Map.entry("stoneType", "stoneType"),
                Map.entry("quantity", "quantity"),
                Map.entry("areaM2", "areaM2"),
                Map.entry("purchaseCost", "purchaseCost"),
                Map.entry("receivedAt", "receivedAt"),
                Map.entry("notes", "notes")
        ), Map.of(
                "quantity", GridFilterKind.NUMBER,
                "areaM2", GridFilterKind.NUMBER,
                "purchaseCost", GridFilterKind.NUMBER,
                "receivedAt", GridFilterKind.DATE
        ));
        register("factory-accept-table", null, Map.ofEntries(
                Map.entry("status", "status"),
                Map.entry("id", "id"),
                Map.entry("orderNo", "orderNo"),
                Map.entry("stoneType", "stoneType"),
                Map.entry("blockCode", "blockCode"),
                Map.entry("machineName", "machineName"),
                Map.entry("responsibleName", "responsibleName"),
                Map.entry("acceptedAt", "acceptedAt")
        ), Map.of(
                "id", GridFilterKind.NUMBER,
                "acceptedAt", GridFilterKind.DATE
        ));
        register("polish-operations-grid", null, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("orderNo", "orderNo"),
                Map.entry("operatorName", "operatorName"),
                Map.entry("blockCode", "blockCode"),
                Map.entry("processLabel", "processLabel"),
                Map.entry("inputQuantity", "inputQuantity"),
                Map.entry("outputQuantity", "outputQuantity"),
                Map.entry("wasteQuantity", "wasteQuantity"),
                Map.entry("chamfer", "chamfer"),
                Map.entry("totalCost", "totalCost"),
                Map.entry("finishedAt", "finishedAt")
        ), Map.of(
                "id", GridFilterKind.NUMBER,
                "inputQuantity", GridFilterKind.NUMBER,
                "outputQuantity", GridFilterKind.NUMBER,
                "wasteQuantity", GridFilterKind.NUMBER,
                "totalCost", GridFilterKind.NUMBER,
                "finishedAt", GridFilterKind.DATE
        ));
    }

    private GridFilterCatalog() {
    }

    public static Map<String, String> aliases(String tableId) {
        Map<String, String> aliases = ALIASES.get(tableId);
        if (aliases == null) {
            return Map.of();
        }
        return aliases;
    }

    public static Class<?> entityType(String tableId) {
        return ENTITIES.get(tableId);
    }

    public static GridFilterKind kind(String tableId, String key) {
        Map<String, GridFilterKind> hints = KINDS.get(tableId);
        if (hints != null && key != null && hints.containsKey(key)) {
            return hints.get(key);
        }
        return GridFilterTypes.kind(entityType(tableId), aliases(tableId).get(key));
    }

    public static Map<String, String> expandAliases(Map<String, String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            if (aliases == null) {
                return Map.of();
            }
            return Map.copyOf(aliases);
        }
        String tableId = tableIdForAliases(aliases);
        if (tableId == null) {
            return Map.copyOf(aliases);
        }
        return aliases(tableId);
    }

    public static Class<?> entityTypeForAliases(Map<String, String> aliases) {
        String tableId = tableIdForAliases(aliases);
        if (tableId == null) {
            return null;
        }
        return entityType(tableId);
    }

    public static boolean allows(String tableId, String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return aliases(tableId).containsKey(key);
    }

    public static Map<String, String> aliasesForEntity(Class<?> entityType) {
        if (entityType == null) {
            return Map.of();
        }
        for (Map.Entry<String, Class<?>> entry : ENTITIES.entrySet()) {
            if (entityType.equals(entry.getValue())) {
                return aliases(entry.getKey());
            }
        }
        return Map.of();
    }

    private static String tableIdForAliases(Map<String, String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            return null;
        }
        String best = null;
        Class<?> entity = null;
        int bestSize = Integer.MAX_VALUE;
        boolean ambiguous = false;
        for (Map.Entry<String, Map<String, String>> entry : ALIASES.entrySet()) {
            if (!containsAll(entry.getValue(), aliases)) {
                continue;
            }
            Class<?> candidate = ENTITIES.get(entry.getKey());
            if (best == null) {
                best = entry.getKey();
                entity = candidate;
                bestSize = entry.getValue().size();
            } else if (candidate != null && entity != null && !candidate.equals(entity)) {
                ambiguous = true;
            } else if (entry.getValue().size() < bestSize) {
                best = entry.getKey();
                entity = candidate;
                bestSize = entry.getValue().size();
            }
        }
        if (ambiguous) {
            return null;
        }
        return best;
    }

    private static boolean containsAll(Map<String, String> catalog, Map<String, String> aliases) {
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (!entry.getValue().equals(catalog.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static void register(String tableId, Class<?> entityType, Map<String, String> aliases) {
        register(tableId, entityType, aliases, Map.of());
    }

    private static void register(String tableId, Class<?> entityType, Map<String, String> aliases,
                                 Map<String, GridFilterKind> kinds) {
        ALIASES.put(tableId, Map.copyOf(aliases));
        if (entityType != null) {
            ENTITIES.put(tableId, entityType);
        }
        if (kinds != null && !kinds.isEmpty()) {
            KINDS.put(tableId, Map.copyOf(kinds));
        }
    }

    private static Map<String, String> merge(Map<String, String> first, Map<String, String> second) {
        Map<String, String> merged = new HashMap<>();
        if (first != null) {
            merged.putAll(first);
        }
        if (second != null) {
            merged.putAll(second);
        }
        return merged;
    }
}
