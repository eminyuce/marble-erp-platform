package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.function.Function;

/**
 * Builds Tabulator remote-pagination {@link Pageable} values, maps grid column
 * names onto persistable entity properties, and retries when a sort field is
 * still not queryable so the API never returns HTML 500s that freeze the grid.
 */
public final class GridPages {

    public static final String DEFAULT_SORT_PROPERTY = "createdDate";

    public static final Map<String, String> COMMON_SORT_ALIASES = Map.of(
            "statusLabel", "status",
            "createdAt", DEFAULT_SORT_PROPERTY
    );

    public static final Map<String, String> BLOCK_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("blockCode", "blockCode"),
            Map.entry("quarryName", "quarry.name"),
            Map.entry("stoneType", "stoneType"),
            Map.entry("qualityGrade", "qualityGrade"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("totalCost", "totalCost"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> PRODUCTION_ORDER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("orderNo", "orderNo"),
            Map.entry("machineName", "machineName"),
            Map.entry("operatorName", "operatorName"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> SLAB_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("slabCode", "slabCode"),
            Map.entry("surfaceAreaM2", "surfaceAreaM2"),
            Map.entry("surfaceFinish", "surfaceFinish"),
            Map.entry("qualityGrade", "qualityGrade"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("costPerM2", "costPerM2"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> CUT_ORDER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("cutOrderNo", "cutOrderNo"),
            Map.entry("projectName", "project.name"),
            Map.entry("locationName", "location.locationName"),
            Map.entry("machineName", "machineName"),
            Map.entry("operatorName", "operatorName"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> PROJECT_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("projectCode", "projectCode"),
            Map.entry("name", "name"),
            Map.entry("customerName", "customerName"),
            Map.entry("contractValue", "contractValue"),
            Map.entry("actualCost", "actualCost"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> SALES_ORDER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("orderNo", "orderNo"),
            Map.entry("customerName", "customer.companyName"),
            Map.entry("orderDate", "orderDate"),
            Map.entry("totalAmount", "totalAmount"),
            Map.entry("paidAmount", "paidAmount"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> PURCHASE_ORDER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("poNumber", "poNumber"),
            Map.entry("supplierName", "supplier.companyName"),
            Map.entry("projectName", "project.name"),
            Map.entry("orderDate", "orderDate"),
            Map.entry("totalAmount", "totalAmount"),
            Map.entry("status", "status"),
            Map.entry("statusLabel", "status"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> USER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("username", "username"),
            Map.entry("email", "email"),
            Map.entry("enabled", "enabled"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> SUPPLIER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("supplierCode", "supplierCode"),
            Map.entry("companyName", "companyName"),
            Map.entry("contactPerson", "contactPerson"),
            Map.entry("supplierType", "supplierType"),
            Map.entry("supplierTypeLabel", "supplierType"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> CUSTOMER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("customerCode", "customerCode"),
            Map.entry("companyName", "companyName"),
            Map.entry("contactPerson", "contactPerson"),
            Map.entry("customerType", "customerType"),
            Map.entry("customerTypeLabel", "customerType"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> MACHINE_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("businessUnit", "businessUnit"),
            Map.entry("businessUnitLabel", "businessUnit"),
            Map.entry("machineType", "machineType"),
            Map.entry("machineTypeLabel", "machineType"),
            Map.entry("active", "active"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> STOCK_LOCATION_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("businessUnit", "businessUnit"),
            Map.entry("businessUnitLabel", "businessUnit"),
            Map.entry("locationType", "locationType"),
            Map.entry("locationTypeLabel", "locationType"),
            Map.entry("active", "active"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> QUARRY_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("location", "location"),
            Map.entry("specificGravity", "specificGravity"),
            Map.entry("licenseNo", "licenseNo"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> COST_CENTER_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("code", "code"),
            Map.entry("name", "name"),
            Map.entry("businessUnit", "businessUnit"),
            Map.entry("businessUnitLabel", "businessUnit"),
            Map.entry("monthlyBudget", "monthlyBudget"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    public static final Map<String, String> EXPENSE_SORTS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("entryDate", "entryDate"),
            Map.entry("invoiceDate", "invoiceDate"),
            Map.entry("amount", "amount"),
            Map.entry("expenseType", "expenseType"),
            Map.entry("expenseTypeLabel", "expenseType"),
            Map.entry("expenseCategory", "expenseCategory"),
            Map.entry("businessUnit", "businessUnit"),
            Map.entry("businessUnitLabel", "businessUnit"),
            Map.entry("costCenterCode", "costCenter.code"),
            Map.entry("costCenterName", "costCenter.name"),
            Map.entry("documentNo", "documentNo"),
            Map.entry("expensePeriod", "expensePeriod"),
            Map.entry("createdDate", DEFAULT_SORT_PROPERTY),
            Map.entry("createdAt", DEFAULT_SORT_PROPERTY)
    );

    private GridPages() {
    }

    public static String normalizeSearch(String search) {
        return Strings.isPresent(search) ? search.trim() : null;
    }

    public static Pageable of(int page, int size, String sortField, String sortDir) {
        return of(page, size, sortField, sortDir, Map.of());
    }

    public static Pageable of(int page, int size, String sortField, String sortDir, Map<String, String> sortAliases) {
        return of(page, size, sortField, sortDir, DEFAULT_SORT_PROPERTY, sortAliases);
    }

    public static Pageable of(int page, int size, String sortField, String sortDir, String defaultProperty) {
        return of(page, size, sortField, sortDir, defaultProperty, Map.of());
    }

    public static Pageable of(int page, int size, String sortField, String sortDir,
                              String defaultProperty, Map<String, String> sortAliases) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : Constants.DEFAULT_PAGE_SIZE;
        return PageRequest.of(pageIndex, pageSize, sort(sortField, sortDir, defaultProperty, sortAliases));
    }

    public static Sort sort(String sortField, String sortDir, String defaultProperty) {
        return sort(sortField, sortDir, defaultProperty, Map.of());
    }

    public static Sort sort(String sortField, String sortDir, String defaultProperty, Map<String, String> sortAliases) {
        String fallback = Strings.isPresent(defaultProperty) ? defaultProperty : DEFAULT_SORT_PROPERTY;
        String property = resolveSortProperty(sortField, fallback, sortAliases);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    public static <T> Page<T> execute(int page, int size, String sortField, String sortDir,
                                      Function<Pageable, Page<T>> query) {
        return execute(of(page, size, sortField, sortDir), query);
    }

    public static <T> Page<T> execute(int page, int size, String sortField, String sortDir,
                                      Map<String, String> sortAliases,
                                      Function<Pageable, Page<T>> query) {
        return execute(of(page, size, sortField, sortDir, sortAliases), query);
    }

    public static <T> Page<T> execute(Pageable pageable, Function<Pageable, Page<T>> query) {
        try {
            return query.apply(pageable);
        } catch (RuntimeException ex) {
            if (!isUnsafeSort(ex) || isDefaultCreatedDateSort(pageable)) {
                throw ex;
            }
            Sort.Direction direction = pageable.getSort().stream()
                    .findFirst()
                    .map(order -> order.getDirection())
                    .orElse(Sort.Direction.DESC);
            Pageable fallback = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(direction, DEFAULT_SORT_PROPERTY));
            return query.apply(fallback);
        }
    }

    static String resolveSortProperty(String sortField, String fallback, Map<String, String> sortAliases) {
        if (!Strings.isPresent(sortField)) {
            return fallback;
        }
        String requestedRaw = sortField.trim();
        if ("createdAt".equalsIgnoreCase(requestedRaw)) {
            return fallback;
        }
        String requested = toEntityProperty(requestedRaw);
        if (!Strings.isPresent(requested)) {
            return fallback;
        }
        String aliased = lookupAlias(requested, requestedRaw, sortAliases);
        if (Strings.isPresent(aliased)) {
            return aliased;
        }
        String common = COMMON_SORT_ALIASES.get(requested);
        return Strings.isPresent(common) ? common : requested;
    }

    private static String lookupAlias(String camelized, String original, Map<String, String> sortAliases) {
        if (sortAliases == null || sortAliases.isEmpty()) {
            return null;
        }
        String aliased = sortAliases.get(camelized);
        if (aliased != null) {
            return aliased;
        }
        return sortAliases.get(original);
    }

    static String toEntityProperty(String sortField) {
        if (sortField.indexOf('_') < 0) {
            return sortField;
        }
        StringBuilder property = new StringBuilder(sortField.length());
        boolean upperNext = false;
        for (int i = 0; i < sortField.length(); i++) {
            char character = sortField.charAt(i);
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                property.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                property.append(character);
            }
        }
        return property.toString();
    }

    static boolean isUnsafeSort(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof PropertyReferenceException
                    || current instanceof InvalidDataAccessApiUsageException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isDefaultCreatedDateSort(Pageable pageable) {
        return pageable.getSort().stream().allMatch(order -> DEFAULT_SORT_PROPERTY.equals(order.getProperty()));
    }
}
