package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum ProcessType {
    GANGSAW("enum.process_type.gangsaw"),
    ST("enum.process_type.st"),
    POLISHING("enum.process_type.polishing"),
    STRIP_POLISHING("enum.process_type.strip_polishing"),
    HONING("enum.process_type.honing"),
    RESIN_LINE("enum.process_type.resin_line"),
    BRIDGE_CUTTING("enum.process_type.bridge_cutting"),
    PALLETIZING("enum.process_type.palletizing");

    private final String messageKey;

    ProcessType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(messageKey);
    }
}
