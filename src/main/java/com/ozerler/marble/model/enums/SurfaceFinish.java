package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum SurfaceFinish {
    RAW("enum.surface_finish.raw"),
    POLISHED("enum.surface_finish.polished"),
    HONED("enum.surface_finish.honed"),
    BRUSHED("enum.surface_finish.brushed"),
    FLAMED("enum.surface_finish.flamed"),
    BUSH_HAMMERED("enum.surface_finish.bush_hammered");

    private final String messageKey;

    SurfaceFinish(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
