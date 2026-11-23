package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import org.springframework.data.domain.Page;

import com.ozerler.marble.dto.EmailTemplateDto;

/**
 * E-posta şablon listesi. The JSON endpoint has no sort keys, so columns stay unsorted.
 */
public final class EmailTemplateGrid {

    private EmailTemplateGrid() {
    }

    public static DataGridView view(Page<EmailTemplateDto> result, int page, int size,
                                    ToIntFunction<EmailTemplateDto> placeholderCount) {
        List<GridRow> rows = new ArrayList<>();
        if (result != null && result.getContent() != null) {
            for (EmailTemplateDto template : result.getContent()) {
                if (template != null) {
                    int count;
                    if (placeholderCount == null) {
                        count = 0;
                    } else {
                        count = placeholderCount.applyAsInt(template);
                    }
                    rows.add(row(template, count));
                }
            }
        }
        int totalPages;
        if (result == null) {
            totalPages = 1;
        } else {
            totalPages = result.getTotalPages();
        }
        long total;
        if (result == null) {
            total = 0L;
        } else {
            total = result.getTotalElements();
        }
        return GridAssembler.from(page, size, totalPages, total, null, null,
                "email-templates-table", 4, columns(), rows);
    }

    public static GridRow row(EmailTemplateDto template, int placeholderCount) {
        String id;
        if (template.getId() == null) {
            id = "";
        } else {
            id = template.getId().toString();
        }
        String detailUrl;
        if (id.isEmpty()) {
            detailUrl = "";
        } else {
            detailUrl = "/admin/email-templates/" + id;
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = detailUrl + "/edit";
        }
        String category = GridTexts.text(template.getCategory());
        if (category.isBlank()) {
            category = "Genel";
        }
        String badge;
        if (Boolean.TRUE.equals(template.getIsActive())) {
            badge = "Aktif";
        } else {
            badge = "Pasif";
        }
        return GridRow.of(detailUrl, editUrl, null, null, List.of(
                GridCell.badge(badge),
                GridCell.chip(template.getId()),
                GridCell.link(template.getTemplateKey(), detailUrl),
                GridCell.link(template.getTemplateName(), detailUrl, description(template.getDescription()), null),
                GridCell.badge(category),
                GridCell.text(template.getSubject()),
                GridCell.text(placeholderCount + " Değişken")
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("active", "Durum", false, GridWidth.PX_110),
                GridColumn.text("id", "Id", false, GridWidth.PX_110),
                GridColumn.text("key", "Şablon Anahtarı", false, GridWidth.PX_190),
                GridColumn.text("name", "Şablon Başlığı", false, GridWidth.PX_260),
                GridColumn.text("category", "Kategori", false, GridWidth.PX_160),
                GridColumn.text("subject", "E-Posta Konusu", false, GridWidth.PX_260),
                GridColumn.text("placeholderCount", "Parametreler", false, GridWidth.PX_130)
        );
    }

    private static String description(String value) {
        String text = GridTexts.text(value);
        if (text.isBlank() || "—".equals(text) || "-".equals(text)) {
            return "";
        }
        return text;
    }
}
