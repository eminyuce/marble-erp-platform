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

    static {
        Map<String, String> blocks = merge(
                GridPages.BLOCK_SORTS,
                Map.ofEntries(
                        Map.entry("locationName", "currentLocation.locationName"),
                        Map.entry("colorTone", "colorTone"),
                        Map.entry("quarrySection", "quarrySection"),
                        Map.entry("customerName", "soldCustomer.companyName"),
                        Map.entry("arrivalDate", "createdDate"),
                        Map.entry("estimatedTonnage", "theoreticalWeightKg"),
                        Map.entry("actualTonnage", "actualWeightKg"),
                        Map.entry("salePrice", "salePrice")
                ));
        register("blocks-table", Block.class, blocks);
        register("factory-blocks-table", Block.class, blocks);
        register("quarry-blocks-table", Block.class, blocks);

        Map<String, String> slabs = merge(
                GridPages.SLAB_SORTS,
                Map.of("customerName", "customer.companyName", "sourceBlockCode", "block.blockCode"));
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

        register("production-table", ProductionOrder.class, GridPages.PRODUCTION_ORDER_SORTS);
        register("workshop-table", CutOrder.class, GridPages.CUT_ORDER_SORTS);
        register("projects-table", Project.class, GridPages.PROJECT_SORTS);
        register("sales-table", SalesOrder.class, GridPages.SALES_ORDER_SORTS);
        register("procurement-table", PurchaseOrder.class, GridPages.PURCHASE_ORDER_SORTS);
        register("users-table", User.class, GridPages.USER_SORTS);
        register("suppliers-table", Supplier.class, GridPages.SUPPLIER_SORTS);
        register("customers-table", Customer.class, merge(
                GridPages.CUSTOMER_SORTS,
                Map.of()));
        register("machines-table", Machine.class, GridPages.MACHINE_SORTS);
        register("stock-locations-table", StockLocation.class, GridPages.STOCK_LOCATION_SORTS);
        register("quarries-table", Quarry.class, GridPages.QUARRY_SORTS);
        register("cost-centers-table", CostCenter.class, GridPages.COST_CENTER_SORTS);
        register("expenses-table", CostTransaction.class, GridPages.EXPENSE_SORTS);

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
                Map.entry("litres", "litres"),
                Map.entry("totalAmount", "totalAmount"),
                Map.entry("workingHoursOrKm", "workingHoursOrKm")
        ));
        register("quarry-dispatches-table", StockMovement.class, Map.ofEntries(
                Map.entry("id", "id"),
                Map.entry("movementDate", "movementDate"),
                Map.entry("notes", "notes")
        ));
        register("workshop-receipts-table", null, Map.of(
                "receiptNo", "receiptNo",
                "supplierName", "supplierName",
                "stoneType", "stoneType",
                "notes", "notes"
        ));
        register("factory-accept-table", null, Map.of(
                "status", "status",
                "orderNo", "orderNo",
                "stoneType", "stoneType",
                "machineName", "machineName"
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

    private static void register(String tableId, Class<?> entityType, Map<String, String> aliases) {
        ALIASES.put(tableId, Map.copyOf(aliases));
        if (entityType != null) {
            ENTITIES.put(tableId, entityType);
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
