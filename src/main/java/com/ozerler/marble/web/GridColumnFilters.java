package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Parses {@code cf.<key>} / {@code co.<key>} query parameters into whitelisted column filters.
 */
public final class GridColumnFilters {

    public static final String VALUE_PREFIX = "cf.";
    public static final String OPERATOR_PREFIX = "co.";
    public static final int MAX_VALUE_LENGTH = 100;

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_]{0,40}");
    private static final int MAX_FILTERS = 10;

    private final List<GridColumnFilter> items;

    private GridColumnFilters(List<GridColumnFilter> items) {
        this.items = List.copyOf(items);
    }

    public static GridColumnFilters none() {
        return new GridColumnFilters(List.of());
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
        return new GridColumnFilters(parsed);
    }

    public static GridColumnFilters fromCurrentRequest(Map<String, String> aliases) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return none();
        }
        return from(attributes.getRequest(), aliases);
    }

    public static GridColumnFilters from(HttpServletRequest request, Map<String, String> aliases) {
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
            String value = first(request, name);
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > MAX_VALUE_LENGTH) {
                trimmed = trimmed.substring(0, MAX_VALUE_LENGTH);
            }
            GridFilterOperator operator = GridFilterOperator.parse(first(request, OPERATOR_PREFIX + key));
            parsed.add(new GridColumnFilter(key, operator, trimmed));
        }
        return new GridColumnFilters(parsed);
    }

    public static boolean isSafeKey(String key) {
        return key != null && KEY_PATTERN.matcher(key).matches();
    }

    public static boolean isFilterParameter(String name) {
        if (name == null) {
            return false;
        }
        return name.startsWith(VALUE_PREFIX) || name.startsWith(OPERATOR_PREFIX);
    }

    public List<GridColumnFilter> getItems() {
        return items;
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

    private static String first(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }
}
