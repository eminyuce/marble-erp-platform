package com.ozerler.marble.model.enums;

import com.ozerler.marble.util.MessageUtils;

public enum BlockStatus {
    QUARRY("enum.block_status.produced"),
    IN_TRANSIT("enum.block_status.dispatched"),
    FACTORY_STOCK("enum.block_status.at_factory"),
    SAWING("enum.block_status.in_process"),
    SOLD("enum.block_status.sold"),
    SCRAPPED("enum.block_status.scrapped"),
    PRODUCED("enum.block_status.produced"),
    MARKED("enum.block_status.marked"),
    DISPATCHED("enum.block_status.dispatched"),
    AT_FACTORY("enum.block_status.at_factory"),
    IN_PROCESS("enum.block_status.in_process");

    private final String messageKey;

    BlockStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLabel() {
        return MessageUtils.getMessage(canonical().messageKey);
    }

    public BlockStatus canonical() {
        return switch (this) {
            case QUARRY -> PRODUCED;
            case IN_TRANSIT -> DISPATCHED;
            case FACTORY_STOCK -> AT_FACTORY;
            case SAWING -> IN_PROCESS;
            default -> this;
        };
    }

    public boolean isAtQuarry() {
        BlockStatus current = canonical();
        return current == PRODUCED || current == MARKED;
    }

    public boolean isAvailableForFactoryAccept() {
        return canonical() == DISPATCHED;
    }

    public boolean isAvailableForCutting() {
        BlockStatus current = canonical();
        return current == AT_FACTORY || current == IN_PROCESS;
    }
}
