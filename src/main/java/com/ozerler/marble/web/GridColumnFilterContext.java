package com.ozerler.marble.web;

import java.util.Map;

/**
 * Request-thread context that tells {@link GridColumnFilterRewriter} which aliases and filters to apply.
 */
public final class GridColumnFilterContext implements AutoCloseable {

    private static final ThreadLocal<GridColumnFilterContext> CURRENT = new ThreadLocal<>();

    private final Map<String, String> aliases;
    private final GridColumnFilters filters;
    private final GridColumnFilterContext previous;

    private GridColumnFilterContext(Map<String, String> aliases, GridColumnFilters filters,
                                    GridColumnFilterContext previous) {
        this.aliases = Map.copyOf(aliases);
        this.filters = filters;
        this.previous = previous;
    }

    public static GridColumnFilterContext open(Map<String, String> aliases) {
        Map<String, String> safe;
        if (aliases == null) {
            safe = Map.of();
        } else {
            safe = aliases;
        }
        return open(safe, GridColumnFilters.fromCurrentRequest(safe));
    }

    public static GridColumnFilterContext open(Map<String, String> aliases, GridColumnFilters filters) {
        Map<String, String> safeAliases;
        if (aliases == null) {
            safeAliases = Map.of();
        } else {
            safeAliases = aliases;
        }
        GridColumnFilters safeFilters;
        if (filters == null) {
            safeFilters = GridColumnFilters.none();
        } else {
            safeFilters = filters;
        }
        GridColumnFilterContext context = new GridColumnFilterContext(safeAliases, safeFilters, CURRENT.get());
        CURRENT.set(context);
        return context;
    }

    public static GridColumnFilterContext current() {
        return CURRENT.get();
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public GridColumnFilters getFilters() {
        return filters;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
