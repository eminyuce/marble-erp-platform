package com.ozerler.marble.web;

import java.util.List;

/**
 * One header cell. Sort and filter are optional and only set when the page already supports them.
 */
public final class GridColumn {

    private final String key;
    private final String label;
    private final boolean sortable;
    private final int minWidth;
    private final String filterParam;
    private final List<FilterChoice> filters;

    public GridColumn(String key, String label, boolean sortable, int minWidth) {
        this(key, label, sortable, minWidth, null, List.of());
    }

    public GridColumn(String key, String label, boolean sortable, int minWidth,
                      String filterParam, List<FilterChoice> filters) {
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
        if (minWidth < 72) {
            this.minWidth = 72;
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
    }

    public static GridColumn text(String key, String label, boolean sortable) {
        return new GridColumn(key, label, sortable, 160);
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
