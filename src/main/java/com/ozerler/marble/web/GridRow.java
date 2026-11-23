package com.ozerler.marble.web;

import java.util.List;

/**
 * One grid body row. The eye link, optional edit link, and optional delete stay in the İşlem column.
 */
public final class GridRow {

    private final String viewUrl;
    private final String editUrl;
    private final String deleteId;
    private final String deleteName;
    private final List<GridCell> cells;

    public GridRow(String viewUrl, String editUrl, String deleteId, String deleteName, List<GridCell> cells) {
        this.viewUrl = viewUrl == null ? "" : viewUrl;
        this.editUrl = editUrl == null ? "" : editUrl;
        this.deleteId = deleteId == null ? "" : deleteId;
        this.deleteName = deleteName == null ? "" : deleteName;
        this.cells = cells == null ? List.of() : List.copyOf(cells);
    }

    public static GridRow of(String viewUrl, String editUrl, Object deleteId, String deleteName, List<GridCell> cells) {
        String id = deleteId == null ? "" : String.valueOf(deleteId);
        return new GridRow(viewUrl, editUrl, id, deleteName, cells);
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public String getEditUrl() {
        return editUrl;
    }

    public String getDeleteId() {
        return deleteId;
    }

    public String getDeleteName() {
        return deleteName;
    }

    public List<GridCell> getCells() {
        return cells;
    }

    public boolean isViewable() {
        return !viewUrl.isBlank();
    }

    public boolean isEditable() {
        return !editUrl.isBlank() && !editUrl.equals(viewUrl);
    }

    public boolean isDeletable() {
        return !deleteId.isBlank();
    }
}
