package com.ozerler.marble.web;

/**
 * Normalizes the query parameters shared by every server-rendered grid.
 */
public final class GridRequests {

    public static final int DEFAULT_SIZE = 20;

    private GridRequests() {
    }

    public static int page(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    public static int size(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size == DEFAULT_SIZE || size == 50 || size == 100) {
            return size;
        }
        return DEFAULT_SIZE;
    }

    public static String sort(String sort, String sortField) {
        if (sort != null && !sort.isBlank()) {
            return sort;
        }
        if (sortField != null && !sortField.isBlank()) {
            return sortField;
        }
        return null;
    }

    public static String direction(String dir, String sortDir) {
        if (dir != null && !dir.isBlank()) {
            return dir;
        }
        if (sortDir != null && !sortDir.isBlank()) {
            return sortDir;
        }
        return "desc";
    }
}
