package com.ozerler.marble.web;

import com.ozerler.marble.dto.EmailTemplateDto;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

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
                    int count = placeholderCount == null ? 0 : placeholderCount.applyAsInt(template);
                    rows.add(row(template, count));
                }
            }
        }
        int totalPages = result == null ? 1 : result.getTotalPages();
        long total = result == null ? 0L : result.getTotalElements();
        return GridAssembler.from(page, size, totalPages, total, null, null,
                "email-templates-table", 4, columns(), rows);
    }

    public static GridRow row(EmailTemplateDto template, int placeholderCount) {
        String id = template.getId() == null ? "" : template.getId().toString();
        String detailUrl = id.isEmpty() ? "" : "/admin/email-templates/" + id;
        String editUrl = id.isEmpty() ? "" : detailUrl + "/edit";
        String category = GridTexts.text(template.getCategory());
        if (category.isBlank()) {
            category = "Genel";
        }
        return GridRow.of(detailUrl, editUrl, null, null, List.of(
                GridCell.badge(Boolean.TRUE.equals(template.getIsActive()) ? "Aktif" : "Pasif"),
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
                GridColumn.text("active", "Durum", false, 110),
                GridColumn.text("id", "Id", false, 110),
                GridColumn.text("key", "Şablon Anahtarı", false, 190),
                GridColumn.text("name", "Şablon Başlığı", false, 260),
                GridColumn.text("category", "Kategori", false, 160),
                GridColumn.text("subject", "E-Posta Konusu", false, 260),
                GridColumn.text("placeholderCount", "Parametreler", false, 130)
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
