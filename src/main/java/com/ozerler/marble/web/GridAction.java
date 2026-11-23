package com.ozerler.marble.web;

/**
 * One item in the shared İŞLEMLER dropdown. Links, delete (with confirm),
 * scripted commands, or a divider.
 */
public final class GridAction {

    private final String label;
    private final String icon;
    private final String href;
    private final String target;
    private final String onclick;
    private final String deleteId;
    private final String deleteName;
    private final boolean danger;
    private final boolean divider;

    private GridAction(String label, String icon, String href, String target, String onclick,
                       String deleteId, String deleteName, boolean danger, boolean divider) {
        this.label = text(label);
        this.icon = text(icon);
        this.href = text(href);
        this.target = text(target);
        this.onclick = text(onclick);
        this.deleteId = text(deleteId);
        this.deleteName = text(deleteName);
        this.danger = danger;
        this.divider = divider;
    }

    public static GridAction link(String label, String icon, String href) {
        return link(label, icon, href, null);
    }

    public static GridAction link(String label, String icon, String href, String target) {
        return new GridAction(label, icon, href, target, null, null, null, false, false);
    }

    public static GridAction command(String label, String icon, String onclick) {
        return new GridAction(label, icon, null, null, onclick, null, null, false, false);
    }

    public static GridAction delete(String label, Object deleteId, String deleteName) {
        String id;
        if (deleteId == null) {
            id = "";
        } else {
            id = String.valueOf(deleteId);
        }
        return new GridAction(label, "trash-2", null, null, null, id, deleteName, true, false);
    }

    public static GridAction divider() {
        return new GridAction("", "", null, null, null, null, null, false, true);
    }

    /**
     * Builds a JavaScript call with string arguments escaped for a single-quoted literal.
     */
    public static String jsCall(String functionName, Object... args) {
        StringBuilder text = new StringBuilder(functionName);
        text.append('(');
        if (args != null) {
            for (int index = 0; index < args.length; index++) {
                if (index > 0) {
                    text.append(", ");
                }
                text.append(jsLiteral(args[index]));
            }
        }
        text.append(')');
        return text.toString();
    }

    static String jsLiteral(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        String raw = String.valueOf(value);
        String escaped = raw.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e");
        return "'" + escaped + "'";
    }

    private static String text(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getHref() {
        return href;
    }

    public String getTarget() {
        if (target.isBlank()) {
            return null;
        }
        return target;
    }

    public String getOnclick() {
        return onclick;
    }

    public String getDeleteId() {
        return deleteId;
    }

    public String getDeleteName() {
        return deleteName;
    }

    public boolean isDanger() {
        return danger;
    }

    public boolean isDivider() {
        return divider;
    }

    public boolean isLink() {
        return !divider && !href.isBlank();
    }

    public boolean isCommand() {
        return !divider && href.isBlank() && deleteId.isBlank() && !onclick.isBlank();
    }

    public boolean isDeletable() {
        return !divider && !deleteId.isBlank();
    }
}
