package com.ozerler.marble.web;

import java.util.List;

/**
 * One header cell. Sort and filter are optional and only set when the page already supports them.
 */
public final class GridColumn {

    private static final int DEFAULT_WIDTH_PX = 160;
    private static final int MIN_WIDTH_PX = 72;

    private final String key;
    private final String label;
    private final boolean sortable;
    private final int minWidth;
    private final String filterParam;
    private final List<FilterChoice> filters;
    private final boolean headerFilter;

    public GridColumn(String key, String label, boolean sortable, int minWidth) {
        this(key, label, sortable, minWidth, null, List.of(), false);
    }

    public GridColumn(String key, String label, boolean sortable, int minWidth,
                      String filterParam, List<FilterChoice> filters) {
        this(key, label, sortable, minWidth, filterParam, filters, false);
    }

    public GridColumn(String key, String label, boolean sortable, int minWidth,
                      String filterParam, List<FilterChoice> filters, boolean headerFilter) {
        if (key == null) {
            this.key = "";
        } else {
            this.key = key;
        }
        if (label == null) {
            this.label = "";
        } else {
            this.label = label;
        }
        this.sortable = sortable && !this.key.isBlank();
        if (minWidth < MIN_WIDTH_PX) {
            this.minWidth = MIN_WIDTH_PX;
        } else {
            this.minWidth = minWidth;
        }
        if (filterParam == null) {
            this.filterParam = "";
        } else {
            this.filterParam = filterParam;
        }
        if (filters == null) {
            this.filters = List.of();
        } else {
            this.filters = List.copyOf(filters);
        }
        this.headerFilter = headerFilter;
    }

    public GridColumn withHeaderFilter(boolean enabled) {
        return new GridColumn(key, label, sortable, minWidth, filterParam, filters, enabled);
    }

    public static GridColumn text(String key, String label, boolean sortable) {
        return new GridColumn(key, label, sortable, DEFAULT_WIDTH_PX);
    }

    public static GridColumn text(String key, String label, boolean sortable, int minWidth) {
        return new GridColumn(key, label, sortable, minWidth);
    }

    public static GridColumn filtered(String key, String label, boolean sortable, int minWidth,
                                      String filterParam, List<FilterChoice> filters) {
        return new GridColumn(key, label, sortable, minWidth, filterParam, filters);
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public boolean isSortable() {
        return sortable;
    }

    public int getMinWidth() {
        return minWidth;
    }

    public String getFilterParam() {
        return filterParam;
    }

    public List<FilterChoice> getFilters() {
        return filters;
    }

    public boolean isFiltered() {
        return !filterParam.isBlank() && !filters.isEmpty();
    }

    public boolean isHeaderFilter() {
        return headerFilter && !key.isBlank();
    }

    public static final class FilterChoice {
        private final String value;
        private final String label;

        public FilterChoice(String value, String label) {
            if (value == null) {
                this.value = "";
            } else {
                this.value = value;
            }
            if (label == null) {
                this.label = "";
            } else {
                this.label = label;
            }
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }
}
