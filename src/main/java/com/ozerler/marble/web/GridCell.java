package com.ozerler.marble.web;

/**
 * One body cell. Empty text renders as a muted dash. Chip text is a monospace id.
 */
public final class GridCell {

    private final String text;
    private final String kind;
    private final String href;
    private final String subtext;
    private final String subHref;

    private GridCell(String text, String kind, String href, String subtext, String subHref) {
        this.text = text == null ? "" : text;
        this.kind = kind == null ? "text" : kind;
        this.href = href;
        this.subtext = subtext == null ? "" : subtext;
        this.subHref = subHref;
    }

    public static GridCell text(String value) {
        return new GridCell(value, "text", null, "", null);
    }

    public static GridCell link(String value, String href) {
        return new GridCell(value, "text", href, "", null);
    }

    public static GridCell link(String value, String href, String subtext, String subHref) {
        return new GridCell(value, "text", href, subtext, subHref);
    }

    public static GridCell badge(String value) {
        return new GridCell(value, "badge", null, "", null);
    }

    public static GridCell chip(Object id) {
        if (id == null) {
            return new GridCell("", "chip", null, "", null);
        }
        String raw = String.valueOf(id).trim();
        if (raw.isEmpty()) {
            return new GridCell("", "chip", null, "", null);
        }
        String shown = raw.charAt(0) == '#' ? raw : "#" + raw;
        return new GridCell(shown, "chip", null, "", null);
    }

    public String getText() {
        return text;
    }

    public String getKind() {
        return kind;
    }

    public String getHref() {
        return href;
    }

    public String getSubtext() {
        return subtext;
    }

    public String getSubHref() {
        return subHref;
    }

    public boolean isEmpty() {
        return text.isBlank();
    }

    public boolean isBadge() {
        return "badge".equals(kind);
    }

    public boolean isChip() {
        return "chip".equals(kind);
    }

    public boolean isLinked() {
        return href != null && !href.isBlank();
    }

    public boolean isSubLinked() {
        return subHref != null && !subHref.isBlank() && !subtext.isBlank();
    }

    public boolean hasSubtext() {
        return !subtext.isBlank();
    }
}
