package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

/**
 * One grid body row. Row actions live in the first İŞLEMLER dropdown column.
 */
public final class GridRow {

    private final String viewUrl;
    private final String editUrl;
    private final String deleteId;
    private final String deleteName;
    private final List<GridAction> actions;
    private final List<GridCell> cells;

    public GridRow(String viewUrl, String editUrl, String deleteId, String deleteName, List<GridCell> cells) {
        this(viewUrl, editUrl, deleteId, deleteName, standardActions(viewUrl, editUrl, deleteId, deleteName), cells);
    }

    public GridRow(String viewUrl, String editUrl, String deleteId, String deleteName,
                   List<GridAction> actions, List<GridCell> cells) {
        if (viewUrl == null) {
            this.viewUrl = "";
        } else {
            this.viewUrl = viewUrl;
        }
        if (editUrl == null) {
            this.editUrl = "";
        } else {
            this.editUrl = editUrl;
        }
        if (deleteId == null) {
            this.deleteId = "";
        } else {
            this.deleteId = deleteId;
        }
        if (deleteName == null) {
            this.deleteName = "";
        } else {
            this.deleteName = deleteName;
        }
        if (actions == null) {
            this.actions = List.of();
        } else {
            this.actions = List.copyOf(actions);
        }
        if (cells == null) {
            this.cells = List.of();
        } else {
            this.cells = List.copyOf(cells);
        }
    }

    public static GridRow of(String viewUrl, String editUrl, Object deleteId, String deleteName, List<GridCell> cells) {
        String id;
        if (deleteId == null) {
            id = "";
        } else {
            id = String.valueOf(deleteId);
        }
        return new GridRow(viewUrl, editUrl, id, deleteName, cells);
    }

    public static GridRow of(List<GridAction> actions, List<GridCell> cells) {
        return new GridRow("", "", "", "", actions, cells);
    }

    static List<GridAction> standardActions(String viewUrl, String editUrl, Object deleteId, String deleteName) {
        List<GridAction> items = new ArrayList<>();
        String view;
        if (viewUrl == null) {
            view = "";
        } else {
            view = viewUrl;
        }
        String edit;
        if (editUrl == null) {
            edit = "";
        } else {
            edit = editUrl;
        }
        boolean hasView = !view.isBlank();
        boolean hasEdit = !edit.isBlank();
        if (hasView && hasEdit && view.equals(edit)) {
            items.add(GridAction.link("Düzenle", "edit-3", edit));
        } else {
            if (hasView) {
                items.add(GridAction.link("Detay", "eye", view));
            }
            if (hasEdit) {
                items.add(GridAction.link("Düzenle", "edit-3", edit));
            }
        }
        if (deleteId != null && !String.valueOf(deleteId).isBlank()) {
            items.add(GridAction.delete("Sil", deleteId, deleteName));
        }
        return items;
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

    public List<GridAction> getActions() {
        return actions;
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

    public boolean isActionable() {
        return !actions.isEmpty();
    }
}
