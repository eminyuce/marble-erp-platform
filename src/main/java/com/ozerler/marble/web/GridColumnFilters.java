package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Parses {@code cf.<key>} / {@code co.<key>} / {@code ct.<key>} into whitelisted column filters.
 * Invalid dates or numbers are ignored and a message is recorded for the page.
 */
public final class GridColumnFilters {

    public static final String VALUE_PREFIX = "cf.";
    public static final String OPERATOR_PREFIX = "co.";
    public static final String TO_PREFIX = "ct.";
    public static final String MESSAGES_ATTRIBUTE = "gridFilterMessages";
    public static final int MAX_VALUE_LENGTH = 100;

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,40}");
    private static final int MAX_FILTERS = 20;

    private final List<GridColumnFilter> items;
    private final List<String> messages;

    private GridColumnFilters(List<GridColumnFilter> items, List<String> messages) {
        this.items = List.copyOf(items);
        this.messages = List.copyOf(messages);
    }

    public static GridColumnFilters none() {
        return new GridColumnFilters(List.of(), List.of());
    }

    public static GridColumnFilters of(GridColumnFilter... items) {
        if (items == null || items.length == 0) {
            return none();
        }
        List<GridColumnFilter> parsed = new ArrayList<>();
        for (GridColumnFilter item : items) {
            if (item != null && item.isActive()) {
                parsed.add(item);
            }
        }
        return new GridColumnFilters(parsed, List.of());
    }

    public static GridColumnFilters fromCurrentRequest(Map<String, String> aliases) {
        return fromCurrentRequest(aliases, GridFilterCatalog.entityTypeForAliases(aliases));
    }

    public static GridColumnFilters fromCurrentRequest(Map<String, String> aliases, Class<?> entityType) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return none();
        }
        return from(attributes.getRequest(), aliases, entityType);
    }

    public static GridColumnFilters from(HttpServletRequest request, Map<String, String> aliases) {
        return from(request, aliases, GridFilterCatalog.entityTypeForAliases(aliases));
    }

    public static GridColumnFilters from(HttpServletRequest request, Map<String, String> aliases,
                                         Class<?> entityType) {
        if (request == null) {
            return none();
        }
        Map<String, String> allowed;
        if (aliases == null) {
            allowed = Map.of();
        } else {
            allowed = aliases;
        }
        List<GridColumnFilter> parsed = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        for (String name : request.getParameterMap().keySet()) {
            if (parsed.size() >= MAX_FILTERS) {
                break;
            }
            if (name == null || !name.startsWith(VALUE_PREFIX)) {
                continue;
            }
            String key = name.substring(VALUE_PREFIX.length());
            if (!isSafeKey(key) || !allowed.containsKey(key)) {
                continue;
            }
            String value = trimToMax(first(request, name));
            String toValue = trimToMax(first(request, TO_PREFIX + key));
            Class<?> javaType = GridFilterTypes.javaType(entityType, allowed.get(key));
            GridFilterKind kind = GridFilterTypes.kindOf(javaType);
            GridFilterOperator operator = GridFilterOperator.parse(
                    first(request, OPERATOR_PREFIX + key), kind);
            GridColumnFilter item = validate(key, operator, kind, value, toValue, messages);
            if (item != null && item.isActive()) {
                parsed.add(item);
            }
        }
        GridColumnFilters filters = new GridColumnFilters(parsed, messages);
        request.setAttribute(MESSAGES_ATTRIBUTE, filters.getMessages());
        return filters;
    }

    public static boolean isSafeKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    public static boolean isFilterParameter(String name) {
        if (name == null) {
            return false;
        }
        return name.startsWith(VALUE_PREFIX)
                || name.startsWith(OPERATOR_PREFIX)
                || name.startsWith(TO_PREFIX);
    }

    public List<GridColumnFilter> getItems() {
        return items;
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isActive(String key) {
        return find(key) != null;
    }

    public GridColumnFilter find(String key) {
        if (key == null) {
            return null;
        }
        for (GridColumnFilter item : items) {
            if (key.equals(item.getKey())) {
                return item;
            }
        }
        return null;
    }

    public String value(String key) {
        GridColumnFilter item = find(key);
        if (item == null) {
            return "";
        }
        return item.getValue();
    }

    public String toValue(String key) {
        GridColumnFilter item = find(key);
        if (item == null) {
            return "";
        }
        return item.getToValue();
    }

    public String operatorCode(String key) {
        GridColumnFilter item = find(key);
        if (item == null) {
            return GridFilterOperator.CONTAINS.getCode();
        }
        return item.getOperator().getCode();
    }

    public Map<String, String> valuesByKey() {
        Map<String, String> map = new LinkedHashMap<>();
        for (GridColumnFilter item : items) {
            map.put(item.getKey(), item.getValue());
        }
        return Map.copyOf(map);
    }

    private static GridColumnFilter validate(String key, GridFilterOperator operator, GridFilterKind kind,
                                             String value, String toValue, List<String> messages) {
        if (kind == GridFilterKind.DATE) {
            return validateDate(key, operator, value, toValue, messages);
        }
        if (kind == GridFilterKind.NUMBER) {
            return validateNumber(key, operator, value, toValue, messages);
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        return new GridColumnFilter(key, operator, value);
    }

    private static GridColumnFilter validateDate(String key, GridFilterOperator operator,
                                                 String value, String toValue, List<String> messages) {
        if (operator == GridFilterOperator.BETWEEN) {
            boolean missing = value == null || value.isBlank() || toValue == null || toValue.isBlank();
            if (missing) {
                if ((value != null && !value.isBlank()) || (toValue != null && !toValue.isBlank())) {
                    messages.add("Aralık için başlangıç ve bitiş tarihi gerekli.");
                }
                return null;
            }
            LocalDate from = GridFilterTypes.parseDate(value);
            LocalDate to = GridFilterTypes.parseDate(toValue);
            if (from == null || to == null) {
                messages.add("Geçersiz tarih, " + key + " filtresi yok sayıldı.");
                return null;
            }
            if (from.isAfter(to)) {
                messages.add("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
                return null;
            }
            return new GridColumnFilter(key, operator, from.toString(), to.toString());
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        LocalDate date = GridFilterTypes.parseDate(value);
        if (date == null) {
            messages.add("Geçersiz tarih, " + key + " filtresi yok sayıldı.");
            return null;
        }
        return new GridColumnFilter(key, operator, date.toString());
    }

    private static GridColumnFilter validateNumber(String key, GridFilterOperator operator,
                                                   String value, String toValue, List<String> messages) {
        if (operator == GridFilterOperator.BETWEEN) {
            boolean missing = value == null || value.isBlank() || toValue == null || toValue.isBlank();
            if (missing) {
                if ((value != null && !value.isBlank()) || (toValue != null && !toValue.isBlank())) {
                    messages.add("Aralık için başlangıç ve bitiş değeri gerekli.");
                }
                return null;
            }
            BigDecimal from = GridFilterTypes.parseNumber(value);
            BigDecimal to = GridFilterTypes.parseNumber(toValue);
            if (from == null || to == null) {
                messages.add("Geçersiz sayı, " + key + " filtresi yok sayıldı.");
                return null;
            }
            if (from.compareTo(to) > 0) {
                messages.add("Başlangıç değeri bitiş değerinden büyük olamaz.");
                return null;
            }
            return new GridColumnFilter(key, operator, from.toPlainString(), to.toPlainString());
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        BigDecimal number = GridFilterTypes.parseNumber(value);
        if (number == null) {
            messages.add("Geçersiz sayı, " + key + " filtresi yok sayıldı.");
            return null;
        }
        return new GridColumnFilter(key, operator, number.toPlainString());
    }

    private static String trimToMax(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_VALUE_LENGTH) {
            return trimmed.substring(0, MAX_VALUE_LENGTH);
        }
        return trimmed;
    }

    private static String first(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }
}
