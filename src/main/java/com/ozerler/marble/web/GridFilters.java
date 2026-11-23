package com.ozerler.marble.web;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Current list filters copied from the request so templates do not read the request parameter map.
 */
public final class GridFilters {

    private final String search;
    private final String status;
    private final String unit;
    private final String type;
    private final String enabled;
    private final String level;
    private final String startDate;
    private final String endDate;
    private final String amount;
    private final List<String> roles;

    private GridFilters(HttpServletRequest request) {
        this.search = text(request, "search");
        this.status = text(request, "status");
        this.unit = text(request, "unit");
        this.type = text(request, "type");
        this.enabled = text(request, "enabled");
        String levels = text(request, "levels");
        String singleLevel = text(request, "level");
        this.level = levels.isBlank() ? singleLevel : levels;
        this.startDate = text(request, "startDate");
        this.endDate = text(request, "endDate");
        this.amount = text(request, "amount");
        String[] selectedRoles = request.getParameterValues("roles");
        this.roles = selectedRoles == null ? List.of() : List.copyOf(Arrays.asList(selectedRoles));
    }

    public static GridFilters from(HttpServletRequest request) {
        return new GridFilters(request);
    }

    public String getSearch() {
        return search;
    }

    public String getStatus() {
        return status;
    }

    public String getUnit() {
        return unit;
    }

    public String getType() {
        return type;
    }

    public String getEnabled() {
        return enabled;
    }

    public String getLevel() {
        return level;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getAmount() {
        return amount;
    }

    public List<String> getRoles() {
        return roles;
    }

    private static String text(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value;
    }
}
