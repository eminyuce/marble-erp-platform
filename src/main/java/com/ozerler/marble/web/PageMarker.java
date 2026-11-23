package com.ozerler.marble.web;

/**
 * One footer page token: a page number or an ellipsis gap.
 */
public final class PageMarker {

    private final int number;
    private final boolean ellipsis;
    private final boolean current;

    private PageMarker(int number, boolean ellipsis, boolean current) {
        this.number = number;
        this.ellipsis = ellipsis;
        this.current = current;
    }

    public static PageMarker page(int number, boolean current) {
        return new PageMarker(number, false, current);
    }

    public static PageMarker ellipsis() {
        return new PageMarker(0, true, false);
    }

    public int getNumber() {
        return number;
    }

    public boolean isEllipsis() {
        return ellipsis;
    }

    public boolean isCurrent() {
        return current;
    }
}
