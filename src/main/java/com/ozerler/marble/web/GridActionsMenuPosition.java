package com.ozerler.marble.web;

/**
 * Desktop placement for the shared İŞLEMLER portal. Keep in sync with
 * {@code positionGridActionsMenu} in {@code static/js/app.js}.
 * The menu's left edge lines up with the trigger and opens down and to the right.
 * It flips up when there is not enough room below, and shifts left only when it
 * would overflow the right edge of the viewport.
 */
public final class GridActionsMenuPosition {

    public static final int GAP = 4;
    public static final int MARGIN = 8;

    private GridActionsMenuPosition() {
    }

    public static int left(int triggerLeft, int menuWidth, int viewportWidth) {
        int left = triggerLeft;
        if (left + menuWidth > viewportWidth - MARGIN) {
            left = viewportWidth - menuWidth - MARGIN;
        }
        if (left < MARGIN) {
            return MARGIN;
        }
        return left;
    }

    public static int top(int triggerTop, int triggerHeight, int menuHeight, int viewportHeight) {
        int top = triggerTop + triggerHeight + GAP;
        if (top + menuHeight > viewportHeight - MARGIN) {
            int flipped = triggerTop - menuHeight - GAP;
            if (flipped >= MARGIN) {
                top = flipped;
            } else {
                top = Math.max(MARGIN, viewportHeight - menuHeight - MARGIN);
            }
        }
        if (top < MARGIN) {
            return MARGIN;
        }
        return top;
    }
}
