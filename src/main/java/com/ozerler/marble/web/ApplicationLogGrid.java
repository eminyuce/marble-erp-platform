package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.ApplicationLogDto;
import com.ozerler.marble.dto.TabulatorResponse;

/**
 * Uygulama log listesi. Sort keys match {@code ApplicationLogService} resolveSortField.
 * Level filter already exists as the {@code levels} query parameter.
 */
public final class ApplicationLogGrid {

    private ApplicationLogGrid() {
    }

    public static DataGridView view(TabulatorResponse<ApplicationLogDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (ApplicationLogDto log : response.getData()) {
                if (log != null) {
                    rows.add(row(log));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "logs-table", 4, columns(), rows);
    }

    public static GridRow row(ApplicationLogDto log) {
        String viewUrl;
        if (log.getId() == null) {
            viewUrl = "";
        } else {
            viewUrl = "#log-" + log.getId();
        }
        String link;
        if (log.isHasException()) {
            link = "Hata";
        } else {
            link = "";
        }
        return GridRow.of(viewUrl, "", null, null, List.of(
                GridCell.badge(log.getLevel()),
                GridCell.chip(log.getId()),
                GridCell.text(when(log)),
                logger(log),
                GridCell.link(log.getMessage(), null, link, null),
                GridCell.text(log.getUsername()),
                GridCell.text(log.getClientIp())
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.filtered("level", "Seviye", true, GridWidth.PX_120, "levels", levels()),
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("timestamp", "Tarih & Saat", true, GridWidth.PX_170),
                GridColumn.text("shortLoggerName", "Kaynak (Logger)", true, GridWidth.PX_180),
                GridColumn.text("message", "Mesaj", false, GridWidth.PX_280),
                GridColumn.text("username", "Kullanıcı", true, GridWidth.PX_140),
                GridColumn.text("clientIp", "İstemci IP", true, GridWidth.PX_130)
        );
    }

    private static List<GridColumn.FilterChoice> levels() {
        return List.of(
                new GridColumn.FilterChoice("", "Tümü"),
                new GridColumn.FilterChoice("ERROR", "ERROR"),
                new GridColumn.FilterChoice("WARN", "WARN"),
                new GridColumn.FilterChoice("INFO", "INFO"),
                new GridColumn.FilterChoice("ERROR,WARN", "Kritik (ERROR & WARN)")
        );
    }

    private static String when(ApplicationLogDto log) {
        String formatted = GridTexts.text(log.getFormattedTimestamp());
        if (!formatted.isBlank()) {
            return formatted;
        }
        return GridTexts.dateTime(log.getTimestamp());
    }

    private static GridCell logger(ApplicationLogDto log) {
        String shortName = GridTexts.text(log.getShortLoggerName());
        String fullName = GridTexts.text(log.getLoggerName());
        if (shortName.isBlank()) {
            return GridCell.text(fullName);
        }
        String detail;
        if (fullName.equals(shortName)) {
            detail = "";
        } else {
            detail = fullName;
        }
        return GridCell.link(shortName, null, detail, null);
    }
}
