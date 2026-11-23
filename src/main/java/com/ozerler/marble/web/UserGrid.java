package com.ozerler.marble.web;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.dto.UserDto;
import com.ozerler.marble.model.enums.UserRole;

/**
 * Kullanıcı listesi. Sort keys match {@code GridPages.USER_SORTS}. Enabled filter already exists.
 */
public final class UserGrid {

    private UserGrid() {
    }

    public static DataGridView view(TabulatorResponse<UserDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (UserDto user : response.getData()) {
                if (user != null) {
                    rows.add(row(user));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "users-table", 4, columns(), rows);
    }

    public static GridRow row(UserDto user) {
        String id = user.getId() == null ? "" : user.getId().toString();
        String viewUrl = id.isEmpty() ? "" : "/admin/users/" + id;
        String editUrl = id.isEmpty() ? "" : viewUrl + "/edit";
        String deleteName = GridTexts.text(user.getFullName());
        if (deleteName.isBlank()) {
            deleteName = GridTexts.text(user.getUsername());
        }
        return GridRow.of(viewUrl, editUrl, user.getId(), deleteName, List.of(
                GridCell.badge(user.isEnabled() ? "Aktif" : "Askıda"),
                GridCell.chip(user.getId()),
                GridCell.link(user.getUsername(), viewUrl),
                GridCell.link(user.getFullName(), viewUrl),
                GridCell.text(user.getEmail()),
                GridCell.text(roleLabels(user.getRoles())),
                GridCell.text(createdOn(user))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("enabled", "Durum", true, 120, "enabled", statuses()),
                GridColumn.text("id", "Id", true, 110),
                GridColumn.text("username", "Kullanıcı Adı", true, 160),
                GridColumn.text("fullName", "Ad Soyad", false, 180),
                GridColumn.text("email", "E-Posta", true, 220),
                GridColumn.text("roles", "Roller", false, 220),
                GridColumn.text("createdAt", "Kayıt Tarihi", true, 140)
        );
    }

    private static List<GridColumn.FilterChoice> statuses() {
        return List.of(
                new GridColumn.FilterChoice("", "Tümü"),
                new GridColumn.FilterChoice("true", "Aktif"),
                new GridColumn.FilterChoice("false", "Askıda")
        );
    }

    private static String roleLabels(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "Rol Yok";
        }
        List<String> labels = new ArrayList<>();
        for (String role : roles) {
            String label = UserRole.labelFor(role);
            if (!label.isBlank()) {
                labels.add(label);
            }
        }
        if (labels.isEmpty()) {
            return "Rol Yok";
        }
        labels.sort((left, right) -> left.compareToIgnoreCase(right));
        return String.join(", ", labels);
    }

    private static String createdOn(UserDto user) {
        LocalDateTime created = user.getCreatedAt() != null ? user.getCreatedAt() : user.getCreatedDate();
        if (created == null) {
            return "";
        }
        return GridTexts.date(created.toLocalDate());
    }
}
